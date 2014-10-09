(ns sm.core
  (:require-macros [wilkerdev.util.macros :refer [<? go-catch dochan]]
                   [cljs.core.async.macros :refer [go]])
  (:require [wilkerdev.util.nodejs :refer [lstat fopen fread http] :as node]
            [wilkerdev.util :as util]
            [wilkerdev.util.reactive :as r]
            [cljs.core.async :refer [<! >! chan close!] :as async]
            [sm.languages :as lang]
            [sm.protocols :as sm]
            [sm.sources.subdb :as subdb]
            [sm.sources.open-subtitles :as os]
            [clojure.string :as str]))

(defn default-sources [] [(subdb/source) (os/source)])

(defn subtitle-name-pattern [path]
  (let [basename (node/basename-without-extension path)
        quoted (util/quote-regexp basename)]
    (js/RegExp (str "^" quoted "(?:\\.([a-z]{2}))?\\.srt$") "i")))

(defn source-url [x] (sm/-linkable-url x))

(defn video-subtitle-languages [path]
  (go-catch
    (let [dirname (node/dirname path)
          files (<? (node/read-dir dirname))
          pattern (subtitle-name-pattern path)
          get-lang (fn [p]
                     (if-let [[_ lang] (re-find pattern p)]
                       (or lang :plain)))]
      (set (keep get-lang files)))))

(defn source-info [source]
  (let [s {:source source :source-name (name source)}]
    (if (sm/linkable? source)
      (assoc s :source-url (source-url source))
      s)))

(defn find-first [{:keys [sources path languages]}]
  (go-catch
    (loop [sources sources]
      (when-let [source (first sources)]
        (let [[res] (<? (sm/search-subtitles source path languages))]
          (if res
            (-> (source-info source)
                (assoc :subtitle res))
            (recur (rest sources))))))))

(defn find-all [{:keys [sources path languages]}]
  (let [search (fn [source]
                 (go-catch
                   (map #(assoc (source-info source) :subtitle %) (<? (sm/search-subtitles source path languages)))))
        searches (map search sources)]
    (r/mapcat (async/merge searches))))

(defn subtitle-target-path [basename lang]
  (if (= lang :plain)
    (str basename ".srt")
    (str basename "." lang ".srt")))

(defn in-memory-cache
  ([] (in-memory-cache (atom {})))
  ([cache]
   (reify
     sm/CacheStore
     (cache-exists? [_ key] (contains? @cache key))
     (cache-store! [_ key] (swap! cache assoc key true)))))

(defn download-subtitle [{:keys [subtitle] :as result} out-stream]
  (go-catch
    (<? (node/pipe-stream (sm/download-stream subtitle) out-stream))
    (-> result
        (assoc :downloaded-path (.-path out-stream)))))

(defn process
  ([query] (process query (chan)))
  ([{:keys [path sources cache]
     :or   {cache (in-memory-cache)}
     :as   query} c]
   (go
     (let [file-hasher (r/memoize-async subdb/hash-file)
           sub-hasher (r/memoize-async node/md5-file)]
       (try
         (>! c [:init])
         (let [subtitles (<? (video-subtitle-languages path))
               basepath (node/basepath path)
               query (update-in query [:languages] #(take-while (complement subtitles) %))]
           (>! c [:info subtitles])
           (>! c [:view-path path])
           (let [file-hash (<? (file-hasher path))]
             (doseq [source (filter sm/upload-provider? sources)
                     lang subtitles
                     :let [sub-path (subtitle-target-path basepath lang)
                           sub-hash (<? (sub-hasher sub-path))
                           cache-key (str/join "-" [file-hash sub-hash (name source)])]]
               (when-not (sm/cache-exists? cache cache-key)
                 (>! c [:upload sub-path])
                 (>! c [:uploaded (<? (sm/upload-subtitle source path sub-path))])
                 (sm/cache-store! cache cache-key))))
           (if (seq (:languages query))
             (do
               (>! c [:search query])
               (if-let [{:keys [subtitle] :as result} (<? (find-first query))]
                 (let [target-path (subtitle-target-path basepath (sm/subtitle-language subtitle))]
                   (>! c [:download (assoc result :target target-path)])
                   (>! c [:downloaded (<? (download-subtitle result (node/create-write-stream target-path)))]))
                 (>! c [:not-found])))
             (>! c [:unchanged])))
         (catch js/Error e (>! c [:error e]))
         (finally (close! c))))
     nil)
   c))

(defn process-alternatives [subtitles]
  (r/into-list (map #(download-subtitle % (node/temp-stream ".srt")) subtitles)))

(defn search-alternatives [{:keys [path] :as query}]
  (let [c (chan 1)
        basepath (node/basepath path)
        af (fn [v c]
             (go
               (let [{subtitle :subtitle :as result} (<! (download-subtitle v (node/temp-stream ".srt")))]
                 (>! c (assoc result :save-path (subtitle-target-path basepath (sm/subtitle-language subtitle)))))
               (close! c)))]
    (async/pipeline-async 5 c af (find-all query))
    c))
