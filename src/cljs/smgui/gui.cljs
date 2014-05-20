(ns smgui.gui
  (:require [cljs.core.async :refer [chan put!]]))

(def nwgui (.require js/window "nw.gui"))

(def open-channel (chan))

(-> nwgui .-App (.on "open" #(smgui.search/search-for %)))

(defn open-external [url]
  (-> nwgui .-Shell (.openExternal url)))

(defn show-file [path]
  (-> nwgui .-Shell (.showItemInFolder path)))
