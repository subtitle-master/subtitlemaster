(ns smgui.main
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om]
            [om.dom :as dom]
            [smgui.components :refer [external-link pd]]
            [smgui.search :refer [render-search]]
            [smgui.settings :as settings]
            [smgui.gui :as gui]
            [smgui.track :as track]
            [smgui.core :refer [app-state flux-channel]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [clojure.data :as data]
            [clojure.string :as string]
            [weasel.repl :as ws-repl]))

(defn render-not-found []
  (dom/div nil "Página não encontrada"))

(defn render-page [cursor]
  (case (:page cursor)
    :search (render-search (get cursor :searches))
    :settings (settings/render-page cursor)
    (render-not-found)))

(defn change-page [cursor new-page]
  (track/screen (name new-page))
  (om/update! cursor [:page] new-page))

(defn page-link [page cursor view]
  (let [class (if (= (:page cursor) page) "button selected" "button")]
    (dom/a #js {:href "#" :className class :onClick (pd #(change-page cursor page))} view)))

(defn updater [cursor _]
  (reify
    om/IRender
    (render [_]
      )))

(defn main-view [cursor _]
  (reify
    om/IWillMount
    (will-mount [_]
      (track/screen (-> cursor :page name)))

    om/IRender
    (render [_]
      (dom/div #js {:className "app-container flex-column"}
        (dom/hr #js {:className "filmstrip shadow-down"})
        (render-page cursor)
        (dom/hr #js {:className "filmstrip shadow-up"})
        (dom/div #js {:className "app-menu flex-row"}
          (page-link :search cursor (dom/img #js {:src "images/icons/magnify.png"}))
          (dom/div #js {:className "flex"})
          (external-link "https://www.facebook.com/subtitlemaster" (dom/img #js {:src "images/icons/facebook.png"}))
          (page-link :settings cursor (dom/img #js {:src "images/icons/gear.png"})))))))

(defn reset-app-to [state]
  (reset! app-state state)
  (om/root main-view app-state {:target (.-body js/document)}))

(defn init []
  (ws-repl/connect "ws://localhost:9001")
  (enable-console-print!)
  (om/root main-view app-state {:target (.-body js/document)})
  (doseq [path gui/app-args]
    (put! flux-channel {:cmd :add-search :path path})))
