(ns smgui.gui
  (:require [cljs.core.async :refer [chan put!]]))

(def nwgui (js/require "nw.gui"))

(def open-channel (chan))

(-> nwgui .-App (.on "open" #(smgui.search/search-for %)))

(defn open-external [url]
  (-> nwgui .-Shell (.openExternal url)))

(defn show-file [path]
  (-> nwgui .-Shell (.showItemInFolder path)))

(def app-args (-> nwgui .-App .-argv array-seq))
