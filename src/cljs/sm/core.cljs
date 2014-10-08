(ns sm.core
  (:require-macros [wilkerdev.util.macros :refer [<? go-catch]]
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

(defn subtitle-info [path]
  (go-catch
    (let [dirname (node/dirname path)
          files (<? (node/read-dir dirname))
          pattern (subtitle-name-pattern path)
          get-lang (fn [p]
                     (if-let [[_ lang] (re-find pattern p)]
                       (or lang :plain)))]
      {:path      path
       :basepath  (node/path-join dirname (node/basename-without-extension path))
       :subtitles (set (keep get-lang files))})))

(defn find-first [{:keys [sources path languages]}]
  (go-catch
    (loop [sources sources]
      (when-let [source (first sources)]
        (let [[res] (<? (sm/search-subtitles source path languages))]
          (if res
            {:subtitle    res
             :source      source
             :source-name (name source)
             :source-url (source-url source)}
            (recur (rest sources))))))))

(defn find-all [{:keys [sources path languages]}]
  (let [searches (map #(sm/search-subtitles % path languages) sources)]
    (async/merge searches)))

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
         (let [{:keys [subtitles basepath] :as info} (<? (subtitle-info path))
               query (update-in query [:languages] #(take-while (complement subtitles) %))]
           (>! c [:info info])
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
                   (<? (node/save-stream-to (sm/download-stream subtitle) target-path))
                   (>! c [:downloaded]))
                 (>! c [:not-found])))
             (>! c [:unchanged])))
         (catch js/Error e (>! c [:error e]))
         (finally (close! c))))
     nil)
   c))
