(ns smgui.engine
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [chan close! put!]]
            [smgui.track :as track]))

(def subtitle-master (.require js/window "subtitle-master"))
(def sm-search (.-SearchDownload subtitle-master))
(def sm-scan (.-VideoScan subtitle-master))
(def sm-alternatives (.-AlternativeSearch subtitle-master))

(defn promise [value]
  #js {:then (fn [callback] (callback value))})

(defn cache-key [hash] (str "upload-cache-" hash))

(defn cache-read [hash]
  (aget (-> js/window .-localStorage) (cache-key hash)))

(defn cache-write [hash]
  (aset (-> js/window .-localStorage) (cache-key hash) true))

(def local-cache
  #js {:check (fn [hash] (promise (cache-read hash)))
       :put (fn [hash] (promise (cache-write hash)))})

(defn put-and-close! [channel message]
  (put! channel message)
  (close! channel))

(defn download [path languages]
  (let [op (sm-search. path (apply array languages) local-cache)
        promise (.run op)
        c (chan)]
    (.then promise
           #(do
             (put-and-close! c [% nil])
             (track/search %))
           #(do
             (put-and-close! c ["error" (.-message %)])
             (track/search "error"))
           #(put! c (array-seq %)))
    c))

(defn read-alternative [obj]
  (let [path (.-path obj)
        source-path (.-sourcePath obj)
        target-path (.-targetPath obj)
        subtitle (.-subtitle obj)
        lang (.language subtitle)
        source (-> subtitle .-source .name)]
    {:path path
     :language lang
     :source source
     :source-path source-path
     :target-path target-path}))

(defn search-alternatives [path languages]
  (let [op (sm-alternatives. path (apply array languages))
        promise (.run op)
        c (chan)]
    (.then promise
           #(do
             (put-and-close! c [:ok (->> %
                                         array-seq
                                         (map read-alternative))])
             (track/search "alternatives"))
           #(put-and-close! c [:error %]))
    c))

(defn scan [path]
  (let [promise (sm-scan #js [path])
        c (chan)]
    (.then promise
           nil
           #(print "Error on scan" %)
           #(put! c (.-value %)))
    c))
