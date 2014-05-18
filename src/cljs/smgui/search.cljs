(ns smgui.search
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.dom :as dom :include-macros true]
            [smgui.engine :refer [download]]
            [cljs.core.async :refer [put! chan <! >! close! map>]]))

(defn define-status [icon detail]
  { :icon icon :detail detail })

(def status-map { :init       (define-status "time"        #(str "Iniciando a busca..."))
                  :info       (define-status "time"        #(str "Carregando informações do vídeo..."))
                  :upload     (define-status "upload"      #(str "Enviando " (:upload %) " ..."))
                  :search     (define-status "search"      #(str "Buscando legendas nos idiomas: " (:search %)))
                  :download   (define-status "download"    #(str "Baixando legenda do servidor: " (get-in % [:download :source :name]) "..."))
                  :downloaded (define-status "check"       #(str "Baixado do servidor " (get-in % [:download :source :name]) " (" (get-in % [:download :source :website]) ")"))
                  :notfound   (define-status "error"       #(str "Nenhuma legenda encontrada, tente novamente mais tarde"))
                  :unchanged  (define-status "check-small" #(str "Você já tem a legenda no seu idioma favorito"))
                  :uploaded   (define-status "check"       #(str "Suas legendas locais para esse vídeo foram compartilhadas!"))
                  :share      (define-status "upload"      #(str "Compartilhando as legendas desse vídeo..."))
                  :error      (define-status "error"       #(str "Erro" (:error %)))})

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

(defn search-from-path [path]
  [path (state-download (download path (smgui.settings/languages)))])

(def path-in-channel (map> search-from-path search-channel))

(defn add-search [path c]
  (put! search-channel [path (state-download c)]))

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
                      (dom/div #js {:className "detail"} detail))
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
