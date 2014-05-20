(ns smgui.engine
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [chan close! put!]]))

(def subtitle-master (.require js/window "subtitle-master"))
(def sm-search (.-SearchDownload subtitle-master))
(def sm-scan (.-VideoScan subtitle-master))

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
           #(put-and-close! c [% nil])
           #(put-and-close! c ["error" %])
           #(put! c (array-seq %)))
    c))

(defn scan [path]
  (let [promise (sm-scan #js [path])
        c (chan)]
    (.then promise
           nil
           #(print "Error on scan" %)
           #(put! c (.-value %)))
    c))
