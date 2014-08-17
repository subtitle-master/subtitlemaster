(ns smgui.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [smgui.settings :as settings]
            [smgui.track :as track]
            [cljs.core.async :refer [chan <! put!]]))

(defn- listen-channel [channel callback]
  (go-loop [data (<! channel)]
           (when data
             (callback data)
             (recur (<! channel)))))

; the atom that represents the app
(def app-state
  (atom {:page :search
         :searches {}
         :settings (settings/read)}))

; save options when it changes
(add-watch app-state :settings (fn [_ _ {old-settings :settings} {new-settings :settings}]
  (if (not= old-settings new-settings)
    (settings/save new-settings))))

; channel for app response flux
(def flux-channel (chan))

(defmulti flux-handler (fn [cmd] (:cmd cmd)))

(defmethod flux-handler :change-page [{:keys [page]}]
  (track/screen (name page))
  (swap! app-state assoc :page page))

(defmethod flux-handler :search-alternatives [{:keys [id channel]}]
  (swap! app-state update-in [:searches id] assoc :alternatives :loading)
  (go (swap! app-state update-in [:searches id] assoc :alternatives (<! channel))))

(defmethod flux-handler :alternatives-close [{:keys [id]}]
  (swap! app-state update-in [:searches id] assoc :alternatives nil))

(defmethod flux-handler :remove-search [{:keys [id]}]
  (swap! app-state update-in [:searches] dissoc id))

(defn call [cmd map]
  (put! flux-channel (assoc map :cmd cmd)))

(listen-channel flux-channel flux-handler)
