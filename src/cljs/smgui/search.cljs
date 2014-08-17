(ns smgui.search
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [swannodette.utils.macros :refer [dochan <? go-catch]])
  (:require [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [smgui.engine :refer [download scan search-alternatives]]
            [smgui.gui :as gui]
            [smgui.core :as app :refer [flux-handler]]
            [smgui.settings :as settings]
            [smgui.util :refer [class-set copy-file]]
            [smgui.dirscan :as dir]
            [cljs.core.async :refer [put! chan <! >! close! pipe]]
            [swannodette.utils.reactive :as r]))

(defn define-status [icon detail]
  { :icon icon :detail detail })

(defn- status-website-link [status]
  (let [url (-> (:download status) .-source .website)]
    (smgui.components/external-link url url)))

(def status-map { :init       (define-status "time"        (fn [] ["Iniciando a busca..."]))
                  :info       (define-status "time"        (fn [] ["Carregando informações do vídeo..."]))
                  :upload     (define-status "upload"      (fn [status] ["Enviando " (:upload status) " ..."]))
                  :search     (define-status "search"      (fn [status] [(str "Buscando legendas nos idiomas: " (:search status))]))
                  :download   (define-status "download"    (fn [status] ["Baixando legenda do servidor: " (-> (:download status) .-source .name) "..."]))
                  :downloaded (define-status "check"       (fn [status] ["Baixado do servidor " (-> (:download status) .-source .name) " (" (status-website-link status) ")"]))
                  :notfound   (define-status "error"       (fn [] ["Nenhuma legenda encontrada, tente novamente mais tarde"]))
                  :unchanged  (define-status "check-small" (fn [] ["Você já tem a legenda no seu idioma favorito"]))
                  :uploaded   (define-status "check"       (fn [] ["Suas legendas locais para esse vídeo foram compartilhadas!"]))
                  :share      (define-status "upload"      (fn [] ["Compartilhando as legendas desse vídeo..."]))
                  :error      (define-status "error"       (fn [{err :error}] ["Erro" err]))})

(defn state-download [in]
  (let [out (chan)]
    (go-loop [state {:status :init}]
             (>! out state)
             (if-let [data (<! in)]
               (let [[sstatus info] data
                     status (keyword sstatus)]
                 (recur (merge state {:status status status info})))
               (close! out)))
    out))

(defn download-chan [path]
  (state-download (download path (smgui.settings/languages))))

(defn status-icon [icon]
  (dom/img #js {:src (str "images/icon-" icon ".svg") :className "status"}))

(defn basename [path]
  (-> (js/require "path")
      (.basename path)))

(defn alternative-item-view [{:keys [path language source target-path]} id]
  (dom/li #js {:className "flex-row"}
    (dom/a #js {:href     path
                :download "subtitle"
                :className "flex"}
               (str language " - " source))
    (dom/button #js {:onClick #(do
                                (copy-file path target-path)
                                (app/call :alternatives-close {:id id}))} "Selecionar")))

(defn alternative-loading []
  (dom/div #js {:className "loader10"}))

(defn alternative-response [[type data] id]
  (case type
    :ok (if (empty? data)
          (dom/p nil "Nenhuma legenda alternativa encontrada")
          (apply dom/ul nil (map #(alternative-item-view % id) data)))
    :error (dom/div "Erro: " (.-message data))))

(defn alternatives-view [a id]
  (let [classes (class-set {"active" (not (nil? a))})]
    (dom/div #js {:className (str "alternatives-box " classes)}
      (dom/div nil
        (case a
          nil nil
          :loading (alternative-loading)
          (alternative-response a id))))))

(defn search-item [search]
  (let [{:keys [status path id viewPath alternatives]} search
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
                                    :onClick #(gui/show-file viewPath)} (dom/img #js {:src "images/icon-view.svg"}))
                      (dom/div #js {:className "alternatives"
                                    :onClick #(app/call :search-alternatives {:id id
                                                                              :channel (search-alternatives path (settings/languages))})} (dom/img #js {:src "images/icon-plus.svg"}))))
       (alternatives-view alternatives id))))

(defn render-search-blank []
  (dom/div #js {:className "flex flex-row"}
           (dom/div #js {:className "center-banner"} "Arraste seus vídeos aqui")))

(defn render-search-list [searches]
  (apply dom/div #js {:className "flex auto-scroll"} (map search-item searches)))

(defn render-search [searches]
  (let [c    (chan)
        view (if (empty? searches)
               (render-search-blank)
               (render-search-list (vals searches)))]
    (pipe (r/map (fn [path] {:cmd :add-search
                             :path path})
                 c)
          app/flux-channel false)
    (om/build smgui.components/file-dropper searches {:state {:view view
                                                              :channel c}})))

(defmethod flux-handler :add-search [{source-path :path}]
  (dochan [{:keys [path channel]} (->> (dir/show-lookup source-path)
                                       (r/map #(hash-map :path % :channel (download-chan %))))]
    (go
      (let [id (rand)]
        (loop [state {:status :init}]
          (when state
            (swap! app/app-state update-in [:searches] #(assoc % id (merge state {:path path :id id})))
            (recur (<! channel))))))))
