(ns smgui.engine
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [chan close! put!]]))

(def subtitle-master (.require js/window "subtitle-master"))
(def sm-search (.-SearchDownload subtitle-master))

(defn put-and-close! [channel message]
  (put! channel message)
  (close! channel))

(defn download [path languages]
  (let [op (sm-search. path (apply array languages))
        promise (.run op)
        c (chan)]
    (.then promise
           #(put-and-close! c [% nil])
           #(put-and-close! c ["error" %])
           #(put! c (array-seq %)))
    c))
