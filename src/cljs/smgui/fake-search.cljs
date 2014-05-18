; this module contains helpers to run fake searches for testing UI
(ns smgui.fake-search
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [timeout <! >! chan close! put!]]
            [smgui.search :refer [add-search]]))

(def fake-status-data {:upload "path.mkv"
                       :search "pt,en"
                       :download {:source {:name "SubDB" :website "www.subdb.net"}}
                       :error "boom!"})

(def fake-flows {:unchanged      [:init :info :unchanged]
                 :upload         [:init :info :upload :share :uploaded]
                 :uploadDownload [:init :info :upload :search :download :downloaded]
                 :download       [:init :info :search :download :downloaded]
                 :missing        [:init :info :search :notfound]
                 :error          [:init :error]})

(defn status-report [status]
  [status (get fake-status-data status)])

(defn populate-channel [c chain]
  (go
    (doseq [status chain]
      (>! c (status-report status))
      (<! (timeout (+ 100 (rand-int 1000)))))
    (close! c)))

(defn random-map-value [map]
  (get map (rand-nth (keys map))))

(defn create-fake-search []
  (let [c (chan)]
    (populate-channel c (random-map-value fake-flows))
    c))

(defn create-random-search []
  (add-search "random-path.mkv" (create-fake-search)))

(defn bomb [n]
  (dorun (- n 1) (repeatedly create-random-search)))

(comment
  (create-random-search))
