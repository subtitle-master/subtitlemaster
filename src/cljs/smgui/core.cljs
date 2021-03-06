(ns smgui.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [smgui.settings :as settings]
            [smgui.track :as track]
            [smgui.engine :as engine]
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
         :retries (or (engine/local-storage-get :retries) #{})
         :organizer {:matched []
                     :searching false}
         :settings (settings/read)}))

; save options when it changes
(add-watch app-state :settings (fn [_ _ {old-settings :settings} {new-settings :settings}]
  (if (not= old-settings new-settings)
    (settings/save new-settings))))

(add-watch app-state :retries (fn [_ _ {old-settings :retries} {new-settings :retries}]
  (if (not= old-settings new-settings)
    (engine/local-storage-set! :retries (get @app-state :retries)))))

; channel for app response flux
(def flux-channel (chan))

(defmulti flux-handler (fn [cmd] (:cmd cmd)))

(defmethod flux-handler :change-page [{:keys [page]}]
  (track/screen (name page))
  (swap! app-state assoc :page page))

(defn call [cmd map]
  (put! flux-channel (assoc map :cmd cmd)))

(listen-channel flux-channel flux-handler)
