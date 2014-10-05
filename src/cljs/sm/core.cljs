(ns sm.core
  (:require-macros [wilkerdev.util.macros :refer [<? go-catch]]
                   [cljs.core.async.macros :refer [go]])
  (:require [wilkerdev.util.nodejs :refer [lstat fopen fread http] :as node]
            [wilkerdev.util :as util]
            [cljs.core.async :refer [<! >! chan close!]]
            [sm.languages :as lang]
            [sm.sources.subdb :as subdb]
            [sm.sources.open-subtitles :as os]
            [clojure.string :as str]))

(defn sources []
  (go-catch
    [(subdb/source) (<? (os/source))]))

(defn subtitle-name-pattern [path]
  (let [basename (node/basename-without-extension path)
        quoted (util/quote-regexp basename)]
    (js/RegExp (str "^" quoted "(?:\\.([a-z]{2}))?\\.srt$") "i")))

(defn subtitle-info [path]
  (go-catch
    (let [dirname (node/dirname path)
          files (<? (node/read-dir dirname))
          pattern (subtitle-name-pattern path)
          get-lang (fn [p]
                     (if-let [[_ lang] (re-find pattern p)]
                       (or lang :plain)))]
      {:path path
       :basepath (node/path-join dirname (node/basename-without-extension path))
       :subtitles (set (keep get-lang files))})))

(defn find-first [{:keys [sources path languages]}]
  (go-catch
    (loop [sources sources]
      (if-let [source (first sources)]
        (let [[res] (<? (search-subtitles source path languages))]
          (if res {:subtitle res :source source} (recur (rest sources))))))))

(defn subtitle-target-path [basename lang]
  (if (= lang :plain)
    (str basename ".srt")
    (str basename "." lang ".srt")))

(defn search-download
  ([query] (search-download query (chan)))
  ([{:keys [path] :as query} c]
   (go
     (try
       (>! c [:init])
       (let [{:keys [subtitles basepath] :as info} (<? (subtitle-info path))
             query (update-in query [:languages] #(take-while (complement subtitles) %))]
         (>! c [:info info])
         (>! c [:view-path path])
         (if (seq (:languages query))
           (do
             (>! c [:search query])
             (if-let [{:keys [subtitle] :as result} (<? (find-first query))]
               (let [target-path (subtitle-target-path basepath (subtitle-language subtitle))]
                 (>! c [:download (assoc result :target target-path)])
                 (<? (node/save-stream-to (download-stream subtitle) target-path))
                 (>! c [:downloaded]))
               (>! c [:not-found])))
           (>! c [:unchanged])))
       (catch js/Error e (>! c [:error e]))
       (finally (close! c)))
     nil)
   c))
