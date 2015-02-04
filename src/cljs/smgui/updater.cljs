(ns smgui.updater
  (:require-macros [wilkerdev.util.macros :refer [<? go-catch]]
                   [cljs.core.async.macros :refer [go]])
  (:require [wilkerdev.util.nodejs :as node]
            [smgui.gui :as gui]
            [smgui.organize :as org]
            [cljs.core.async :as async :refer [chan]]))

(def manifest-url "https://raw.githubusercontent.com/subtitle-master/subtitlemaster/master/latest.json")
(def nw-updater (js/require "node-webkit-updater"))

(defn json-decode [str] (.parse js/JSON str))

(defn current-manifest []
  (go-catch
    (-> (node/http {:uri manifest-url}) <?
        .-body
        json-decode
        js->clj
        (assoc :version node/package-version)
        clj->js)))

(defn lift-instance [instance & methods]
  (doseq [m (map name methods)
          :let [original (aget instance m)]]
    (aset instance m (node/node-lift (.bind original instance))))
  instance)

(defn create-updater [manifest]
  (-> (nw-updater. manifest)
      (lift-instance :checkNewVersion :download :unpack :install)))

(defn updater-unpack [updater filename manifest]
  (let [c (chan)]
    (.unpack updater filename (node/node-callback c) manifest)
    c))

(defn call-async [instance method & args]
  (let [c (chan)
        args (conj (vec args) (node/node-callback c))
        m (name method)
        f (.bind (aget instance m) instance)]
    (apply f args)
    c))

(defn updater-main []
  (go
    (try
      ; TODO: improve update detection
      (let [args (remove org/has-video-extension? gui/app-args)]
        (let [manifest (<? (current-manifest))
              _ (.log js/console "Manifest" manifest)
              updater (create-updater manifest)]
          (if (= (count args) 2)
            (let [[copy-path exec-path] args]
              (.log js/console "Install new updater")
              (<? (.install updater copy-path))
              (.run updater exec-path)
              (gui/quit))
            (when (<? (.checkNewVersion updater))
              (let [_ (.log js/console "Found update, downloading")
                    filename (<? (.download updater))
                    _ (.log js/console "Downloaded, unpacking" filename)
                    new-app-path (<? (updater-unpack updater filename manifest))
                    _ (.log js/console "Unpacked, running updater")]
                (.runInstaller updater
                               new-app-path
                               (clj->js [(.getAppPath updater) (.getAppExec updater)])
                               #js {})
                (gui/quit))))))
      (catch js/Error e
        (.log js/console "got error" (.-stack e))))))
