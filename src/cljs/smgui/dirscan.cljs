(ns smgui.dirscan
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [swannodette.utils.macros :refer [dochan <? go-catch]])
  (:require [cljs.core.async :refer [chan <! >! put! close!]]
            [swannodette.utils.reactive :as r]))

(def nodepath (js/require "path"))
(def dir-separator (.-sep nodepath))
(def fs (js/require "fs"))
(def test-path "/Volumes/WilkerWD/Downloads")

(defn log [& params]
  (.apply (.-log js/console) js/console (clj->js params))
  (last params))

(defn make-js-error [node-err]
  (log "node err", node-err)
  (js/Error. (.-message node-err)))

(defn node->chan [f & args]
  (let [c (chan)
        callback (fn [err res]
                   (if-not err
                     (put! c res)
                     (put! c (make-js-error err)))
                   (close! c))
        args (conj (vec args) callback)]
    (apply f args)
    c))

(defn is-dir? [path]
  (go-catch
   (let [stat (<? (node->chan (.-lstat fs) path))]
     (.isDirectory stat))))

(defn is-file? [path]
  (go-catch
   (let [stat (<? (node->chan (.-lstat fs) path))]
     (.isFile stat))))

(defn match-extensions? [path extensions]
  (extensions (-> (.extname nodepath path)
                  (subs 1))))

(defn readdir [path]
  (let [fullpath (partial str path dir-separator)]
    (go-catch
     (->> (<? (node->chan (.-readdir fs) path))
          array-seq
          (map fullpath)))))

(defn scanpaths
  ([paths] (scanpaths paths (chan)))
  ([input-paths out]
     (go
       (let [paths (atom (vec input-paths))]
         (while @paths
           (try
             (let [path (peek @paths)]
               (swap! paths next)
               (>! out path)
               (if (<? (is-dir? path)) (swap! paths into (<? (readdir path)))))
             (catch js/Error e
               (.error js/console "Node Err:" e))))
         (close! out)))
     out))

(defn scandir
  ([path] (scandir path (chan)))
  ([path out] (scanpaths [path] out)))

(defn has-video-extension? [path] (match-extensions? path #{"mkv" "avi"}))

(defn show-lookup [path]
  (->> (scandir path)
       (r/filter is-file?)
       (r/filter has-video-extension?)))

#_ (go
     (try
       (log (<? (is-dir? test-path)))
       (catch js/Error e
         (.error js/console "Node:" e))))
#_ (go
  (log (<! (dochan [file (show-lookup test-path)]
                   (log file)))))

#_ (go (log (<! (is-dir? test-path))))
