(ns smgui.search
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.dom :as dom :include-macros true]
            [smgui.engine :refer [download]]
            [cljs.core.async :refer [put! chan <! >! close! map>]]))

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

(def search-channel (chan))

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

(defn add-search [path c]
  (put! search-channel [path (state-download c)]))

(defn search-for [path]
  (add-search path (smgui.engine/download path (smgui.settings/languages))))

(defn status-icon [icon]
  (dom/img #js {:src (str "images/icon-" icon ".svg") :className "status"}))

(defn search-item [search]
  (let [{:keys [status path id]} search
        status-tr (get status-map status)
        icon (:icon status-tr)
        detail ((:detail status-tr) search)]
    (dom/div #js {:className (str "search flex-row " (name status))}
             (status-icon icon)
             (dom/div #js {:className "info flex"}
                      (dom/div #js {:className "path"} path)
                      (apply dom/div #js {:className "detail"} detail))
             (dom/div #js {:className "actions"}
                      (dom/div #js {:className "close" :onClick #(smgui.core/remove-search id)} (dom/img #js {:src "images/icon-close.svg"}))
                      (dom/div #js {:className "view"} (dom/img #js {:src "images/icon-view.svg"}))))))

(defn render-search-blank []
  (dom/div #js {:className "flex flex-row"}
           (dom/div #js {:className "center-banner"} "Arraste seus vídeos aqui")))

(defn render-search-list [searches]
  (apply dom/div #js {:className "flex auto-scroll"} (map search-item searches)))

(defn render-search [searches]
  (if (empty? searches)
    (render-search-blank)
    (render-search-list (vals searches))))
