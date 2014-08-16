(ns smgui.components
  (:require [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [smgui.util :refer [class-set]]
            [smgui.gui :as gui]
            [cljs.core.async :refer [>!]]))

(defn pd
  ([]
    (pd identity))
  ([f]
    (fn [e]
      (.preventDefault e)
      (f e))))

(defn external-link [url view]
  (dom/a #js {:href "#" :className "button" :onClick (pd #(gui/open-external url))} view))

(defn input-value-seq [input]
  (-> (.$ js/window input)
      .val
      (or [])
      array-seq
      (or [])))

; global cancel default drop behavior
(set! (.-ondragover js/window) #(.preventDefault %))
(set! (.-ondrop js/window) #(.preventDefault %))

(defn- read-file-paths [e]
  (->> e
       .-dataTransfer
       .-files
       array-seq
       (map #(.-path %))
       vec))

(defn file-dropper [_ owner]
  (reify
    om/IInitState
    (init-state [_] {:over false :onFiles (fn [_])})

    om/IRenderState
    (render-state [_ {:keys [over view onFiles] :as state}]
      (let [update-over #(om/set-state! owner :over %)
            classes (class-set {"dragging" over})]
        (dom/div #js {:className   (str "flex flex-row " classes)
                      :onDragEnter (pd #(update-over true))
                      :onDragOver  (pd #(update-over true))
                      :onDragLeave (pd #(update-over false))
                      :onDragEnd   (pd #(update-over false))
                      :onDrop      (pd #(do (update-over false) (onFiles (read-file-paths %))))}
                 view)))))
