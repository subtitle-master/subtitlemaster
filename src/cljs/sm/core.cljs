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
        (assoc :downloaded-path (.-path out-stream))
        (assoc :language (sm/subtitle-language subtitle)))))

(defn gen-cache-key [{:keys [path file-hasher sub-hasher]
                      :or   {file-hasher subdb/hash-file
                             sub-hasher  node/md5-file}} sub-path source]
  (go-catch
    (let [file-hash (<? (file-hasher path))
          sub-hash (<? (sub-hasher sub-path))]
      (str/join "-" [file-hash sub-hash (name source)]))))

(defn- p-read-available-subtitles [{:keys [path languages] :as query}]
  (go-catch
    (let [subtitles (<? (video-subtitle-languages path))]
      (-> query
          (assoc :available-subtitles subtitles)
          (assoc :search-languages (take-while (complement subtitles) languages))))))

(defn- p-upload-subtitle [notify]
  (fn [{:keys [path basepath cache] :as query} [source lang]]
    (go-catch
      (let [sub-path (subtitle-target-path basepath lang)
            cache-key (<? (gen-cache-key query sub-path source))]
        (if-not (sm/cache-exists? cache cache-key)
          (do
            (sm/cache-store! cache cache-key)
            (-> query
                (notify :upload sub-path) <!
                (update-in [:uploads] conj (<? (sm/upload-subtitle source path sub-path)))
                (notify :uploaded) <!))
          query)))))

(defn- p-upload-local-subtitles
  [{:keys [sources available-subtitles] :as query} notify]
  (let [variants (for [s (filter sm/upload-provider? sources)
                       l available-subtitles] [s l])]
    (r/reduce (p-upload-subtitle notify) query (r/spool variants))))

(defn- p-download [{:keys [download basepath] :as query}]
  (go-catch
    (let [subtitle (:subtitle download)
          target-path (subtitle-target-path basepath (sm/subtitle-language subtitle))
          download (<? (download-subtitle download (node/create-write-stream target-path)))]
      (-> query
          (update-in [:available-subtitles] conj (sm/subtitle-language subtitle))
          (assoc :download download)
          (assoc :view-path (:downloaded-path download))))))

(defn- p-run-search [{:keys [search-languages cache] :as query} notify]
  (go-catch
    (if (seq search-languages)
      (do
        (<! (notify query :search))
        (if-let [result (<? (find-first query))]
          (let [query (-> query
                          (assoc :download result)
                          (notify :download) <!
                          (p-download) <?)]
            (sm/cache-store! cache (<? (gen-cache-key query
                                                      (:view-path query)
                                                      (get-in query [:download :source]))))
            (-> query
                (p-upload-local-subtitles notify) <?
                (notify :downloaded) <!))
          (<! (notify query :not-found))))
      (<! (notify query :unchanged)))))

(defn process
  ([query] (process query (chan)))
  ([{:keys [path] :as query} c]
   (go
     (let [notify (fn [q s & [e]] (go (>! c [s q e]) q))]
       (try
         (-> query
             (update-in [:cache] #(or % (in-memory-cache)))
             (assoc :file-hasher (r/memoize-async subdb/hash-file))
             (assoc :sub-hasher (r/memoize-async node/md5-file))
             (assoc :basepath (node/basepath path))
             (assoc :view-path path)
             (notify :init) <!
             (p-read-available-subtitles) <?
             (notify :info) <!
             (p-upload-local-subtitles notify) <?
             (p-run-search notify) <?)
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
               (try
                 (r/throw-err v)
                 (let [{subtitle :subtitle :as result} (<? (download-subtitle v (node/temp-stream ".srt")))]
                   (>! c (assoc result :save-path (subtitle-target-path basepath (sm/subtitle-language subtitle)))))
                 (catch js/Error e
                   (.log js/console "Error" e))
                 (finally
                   (close! c)))))]
    (async/pipeline-async 5 c af (find-all query))
    c))
