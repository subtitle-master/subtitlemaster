(ns smgui.search
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [wilkerdev.util.macros :refer [dochan <? go-catch]])
  (:require [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [smgui.engine :as engine]
            [smgui.gui :as gui]
            [smgui.core :as app :refer [flux-handler app-state]]
            [smgui.settings :as settings]
            [smgui.util :refer [class-set copy-file]]
            [smgui.track :as track]
            [smgui.fs :as fs]
            [smgui.organize]
            [sm.core :as sm]
            [cljs.core.async :refer [put! chan <! >! close! pipe] :as async]
            [wilkerdev.util.reactive :as r]
            [wilkerdev.util.nodejs :as node]))

(defn define-status [icon detail]
  { :icon icon :detail detail })

(def worker-pool (r/channel-pool 5))

(defn- status-website-link [status]
  (let [url (-> status :download :source-url)]
    (smgui.components/external-link url url)))

(def status-map { :init       (define-status "time"        (fn [] ["Iniciando a busca..."]))
                  :info       (define-status "time"        (fn [] ["Carregando informações do vídeo..."]))
                  :upload     (define-status "upload"      (fn [status] ["Enviando " (:extra status) " ..."]))
                  :search     (define-status "search"      (fn [status] [(str "Buscando legendas nos idiomas: " (get-in status [:search-languages]))]))
                  :download   (define-status "download"    (fn [status] ["Baixando legenda do servidor: " (-> status :download :source-name) "..."]))
                  :downloaded (define-status "check"       (fn [status] ["Baixado do servidor " (-> status :download :source-name) " (" (status-website-link status) ")"]))
                  :not-found  (define-status "error"       (fn [] ["Nenhuma legenda encontrada, tente novamente mais tarde"]))
                  :unchanged  (define-status "check-small" (fn [] ["Você já tem a legenda no seu idioma favorito"]))
                  :uploaded   (define-status "check"       (fn [] ["Suas legendas locais para esse vídeo foram compartilhadas!"]))
                  :share      (define-status "upload"      (fn [] ["Compartilhando as legendas desse vídeo..."]))
                  :error      (define-status "error"       (fn [{err :error}] (.log js/console "Error" (clj->js err)) ["Error: " (.-message err)]))})

(def trackable-states #{:init :downloaded :not-found :unchanged :error :uploaded})

(def retry-states #{:not-found :error})

(defn retry-later? [{status :status {lang :language} :download [preferred-lang] :languages :as query}]
  (if (retry-states status)
    true
    (and (= :downloaded status) (not= lang preferred-lang))))

(defn state-download [in]
  (let [out (chan)]
    (go-loop [state {:status :init}]
             (>! out state)
             (if-let [[status info extra] (<! in)]
               (let [new-info (-> info
                                  (assoc :status (if (contains? status-map status)
                                                   status
                                                   (:status state)))
                                  (assoc :extra extra))]
                 (if (trackable-states status) (track/search (name status)))
                 (recur new-info))
               (do
                 (if (retry-later? state)
                   (put! app/flux-channel {:cmd :retry-later :path (:path state)}))
                 (close! out))))
    out))

(def cache-storage (engine/local-storage-cache))

(def cached-sources (sm/default-sources))

(defn download-chan [path]
  (state-download (->> (partial sm/process
                                {:path      path
                                 :sources   cached-sources
                                 :languages (smgui.settings/languages)
                                 :cache     cache-storage}
                                (chan 1))
                       (r/pool-enqueue worker-pool))))

(defn status-icon [icon]
  (dom/img #js {:src (str "images/icon-" icon ".svg") :className "status"}))

(defn basename [path]
  (-> (js/require "path")
      (.basename path)))

(defn alternative-item-view [{:keys [downloaded-path language source-name save-path]} id]
  (dom/li #js {:className "flex-row"}
    (dom/a #js {:href     downloaded-path
                :download "subtitle"
                :className "flex"}
               (str language " - " source-name))
    (dom/button #js {:onClick #(do
                                (copy-file downloaded-path save-path)
                                (app/call :alternatives-close {:id id}))} "Selecionar")))

(defn alternative-loading []
  (dom/div #js {:className "loader10"}))

(defn alternative-response [data id]
  (if (empty? data)
    (dom/p nil "Nenhuma legenda alternativa encontrada")
    (apply dom/ul nil (map #(alternative-item-view % id) data))))

(defn alternatives-view [a id]
  (let [classes (class-set {"active" (not (nil? a))})]
    (dom/div #js {:className (str "alternatives-box " classes)}
      (dom/div nil
        (case a
          nil nil
          :loading (alternative-loading)
          (alternative-response a id))))))

(defn search-item [search]
  (let [{:keys [status path id view-path alternatives]} search
        status-tr (get status-map status)
        icon (:icon status-tr)
        detail ((:detail status-tr) search)]
    (dom/div #js {:style #js {:clear "both"}}
       (dom/div #js {:className (str "search flex-row " (name status))}
             (status-icon icon)
             (dom/div #js {:className "info flex"}
                      (dom/div #js {:className "path"} (basename path))
                      (apply dom/div #js {:className "detail"} detail))
             (dom/div #js {:className "actions"}
                      (dom/div #js {:className "close"
                                    :onClick #(app/call :remove-search {:id id})} (dom/img #js {:src "images/icon-close.svg"}))
                      (dom/div #js {:className "view"
                                    :onClick #(gui/show-file view-path)} (dom/img #js {:src "images/icon-view.svg"}))
                      (dom/div #js {:className "alternatives"
                                    :onClick #(app/call :search-alternatives {:id id
                                                                              :channel (sm/search-alternatives {:path path
                                                                                                                :languages (settings/languages)
                                                                                                                :sources (sm/default-sources)})})} (dom/img #js {:src "images/icon-plus.svg"}))))
       (alternatives-view alternatives id))))

(defn render-retry [retries]
  (let [render (fn [path]
                 (dom/div nil
                   (node/basename path)
                   (dom/button #js {:onClick (fn [_] (put! app/flux-channel {:cmd :retry :path path}))} "Tentar Novamente")
                   (dom/button #js {:onClick (fn [_] (put! app/flux-channel {:cmd :retry-remove :path path}))} "X")))
        search-all (dom/button #js {:onClick (fn [_]
                                               (put! app/flux-channel {:cmd :retry-all}))}
                               "Procurar todos")]
    (if (seq retries)
      (apply dom/div #js {:className "white-box"} (conj (mapv render retries) search-all)))))

(defn render-search-blank [retries]
  (dom/div #js {:className "flex flex-column"}
    (dom/div #js {:className "flex flex-row"}
      (dom/div #js {:className "center-banner"} "Arraste seus vídeos aqui"))
      (render-retry retries)))

(defn render-search-list [searches]
  (apply dom/div #js {:className "flex auto-scroll"} (map search-item searches)))

(defn render-search [searches retries]
  (let [c    (chan)
        view (if (empty? searches)
               (render-search-blank retries)
               (render-search-list (vals searches)))]
    (pipe (r/map (fn [path] {:cmd :add-search
                             :path path})
                 c)
          app/flux-channel false)
    (om/build smgui.components/file-dropper searches {:state {:view view
                                                              :channel c}})))

(defn show-lookup [path]
  (->> (fs/scandir path)
       (r/filter smgui.organize/has-video-extension?)
       (r/filter fs/is-file?)))

; register application handlers
(defmethod flux-handler :add-search [{source-path :path}]
  (dochan [{:keys [path channel]} (->> (show-lookup source-path)
                                       (r/map #(hash-map :path % :channel (download-chan %))))]
    (go
      (let [id (rand)]
        (loop [state {:status :init}]
          (when state
            (swap! app-state update-in [:searches] #(assoc % id (merge state {:path path :id id})))
            (recur (<! channel))))))))

(defmethod flux-handler :search-alternatives [{:keys [id channel]}]
  (track/search "alternatives")
  (swap! app-state update-in [:searches id] assoc :alternatives :loading)
  (go (swap! app-state update-in [:searches id] assoc :alternatives (<! (async/into [] channel)))))

(defmethod flux-handler :alternatives-close [{:keys [id]}]
  (swap! app-state update-in [:searches id] assoc :alternatives nil))

(defmethod flux-handler :remove-search [{:keys [id]}]
  (swap! app-state update-in [:searches] dissoc id))

(defmethod flux-handler :retry [{:keys [path]}]
  (swap! app-state update-in [:retries] disj path)
  (engine/local-storage-set! :retries (get @app-state :retries))
  (put! app/flux-channel {:cmd :add-search :path path}))

(defmethod flux-handler :retry-remove [{:keys [path]}]
  (swap! app-state update-in [:retries] disj path)
  (engine/local-storage-set! :retries (get @app-state :retries)))

(defmethod flux-handler :retry-all [_]
  (let [c (->> (r/spool (get @app-state :retries))
               (r/map #(hash-map :cmd :retry :path %)))]
    (pipe c app/flux-channel false)))

(defmethod flux-handler :retry-later [{:keys [path]}]
  (swap! app-state update-in [:retries] conj path)
  (engine/local-storage-set! :retries (get @app-state :retries)))
