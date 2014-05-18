(ns smgui.components
  (:require [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [smgui.gui :as gui]))

(defn pd [f]
  "Prevent default event handler"
  (fn [e]
    (.preventDefault e)
    (f e)))

(defn external-link [url view]
  (dom/a #js {:href "#" :className "button" :onClick (pd #(gui/open-external url))} view))

(defn input-value-seq [input]
  (-> (.$ js/window input) .val (or []) array-seq (or [])))
