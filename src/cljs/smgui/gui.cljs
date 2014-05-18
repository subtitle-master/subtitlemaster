(ns smgui.gui
  (:require [cljs.core.async :refer [chan put!]]))

(def nwgui (.require js/window "nw.gui"))

(def open-channel (chan))

(-> nwgui .-App (.on "open" #(put! open-channel %)))

(defn open-external [url]
  (-> nwgui .-Shell (.openExternal url)))
