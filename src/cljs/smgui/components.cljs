(ns smgui.components
  (:require [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [smgui.util :refer [class-set]]
            [smgui.gui :as gui]
            [cljs.core.async :refer [>! pipe]]
            [swannodette.utils.reactive :as r]))

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
    (init-state [_] {:over false})

    om/IRenderState
    (render-state [_ {:keys [over view channel] :as state}]
      (let [update-over #(om/set-state! owner :over %)
            classes (class-set {"dragging" over})
            on-files #(pipe (r/spool %) channel false)]
        (dom/div #js {:className   (str "flex flex-row " classes)
                      :onDragEnter (pd #(update-over true))
                      :onDragOver  (pd #(update-over true))
                      :onDragLeave (pd #(update-over false))
                      :onDragEnd   (pd #(update-over false))
                      :onDrop      (pd #(do (update-over false) (on-files (read-file-paths %))))}
                 view)))))
