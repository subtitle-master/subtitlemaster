(ns smgui.dirscan
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [swannodette.utils.macros :refer [dochan]])
  (:require [cljs.core.async :refer [chan <! >! put! close!]]
            [swannodette.utils.reactive :as r]))

(def nodepath (js/require "path"))
(def dir-separator (.-sep nodepath))
(def fs (js/require "fs"))
(def test-path "/Volumes/WilkerWD/Downloads/Torrent")

(defn log [& params]
  (.apply (.-log js/console) js/console (clj->js params))
  (last params))

(defn node->chan [f & args]
  (let [c (chan)
        callback (fn [err res]
                    (if-not err
                      (put! c res)
                      (.error js/console "Node call error:" err))
                    (close! c))
        args (conj (vec args) callback)]
    (apply f args)
    c))

(defn is-dir? [path]
  (go
    (if-let [stat (<! (node->chan (.-lstat fs) path))]
      (.isDirectory stat)
      false)))

(defn is-file? [path]
  (go
    (if-let [stat (<! (node->chan (.-lstat fs) path))]
      (.isFile stat)
      false)))

(defn match-extensions? [path extensions]
  (extensions (-> (.extname nodepath path)
                  (subs 1))))

(defn readdir [path]
  (let [fullpath (partial str path dir-separator)]
    (go
      (if-let [files (<! (node->chan (.-readdir fs) path))]
        (->> files array-seq (map fullpath))))))

(defn scandir
  ([path] (scandir path (chan)))
  ([path out]
     (go
       (let [paths (atom [path])]
         (while @paths
           (let [path (peek @paths)]
             (swap! paths next)
             (let [files (<! (readdir path))]
               (doseq [f files]
                 (>! out f)
                 (if (<! (is-dir? f)) (swap! paths conj f))))))
         (close! out)))
     out))

(defn has-video-extension? [path] (match-extensions? path #{"mkv" "avi"}))

(defn show-lookup [path]
  (->> (scandir test-path)
       (r/filter is-file?)
       (r/filter has-video-extension?)))

(dochan [file (show-lookup test-path)]
  (log file))

(go (log (<! (is-dir? test-path))))
