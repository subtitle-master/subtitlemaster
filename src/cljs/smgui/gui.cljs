(ns smgui.gui
  (:require [cljs.core.async :refer [chan put!]]))

(def nwgui (js/require "nw.gui"))

(def Menu (.-Menu nwgui))
(def Window (.-Window nwgui))

(defn setup-mac-menu []
  (let [mb (Menu. #js {:type "menubar"})
        win (.get Window)]
    (.createMacBuiltin mb "Subtitle Master")
    (set! (.-menu win) mb)))

(setup-mac-menu)

(defn open-channel
  ([] (open-channel (chan)))
  ([out] (-> nwgui
             .-App
             (.on "open" #(put! out %)))))

(defn listen-open [cb]
  (-> nwgui .-App (.on "open" cb)))

(defn open-external [url]
  (-> nwgui .-Shell (.openExternal url)))

(defn show-file [path]
  (-> nwgui .-Shell (.showItemInFolder path)))

(def app-args (array-seq (.. nwgui -App -argv)))

(defn quit [] (.. nwgui -App quit))
