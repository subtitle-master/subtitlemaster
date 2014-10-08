(ns smgui.fs
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [wilkerdev.util.macros :refer [dochan <? go-catch]])
  (:require [cljs.core.async :refer [chan <! >! put! close!]]
            [smgui.path :as path]
            [wilkerdev.util.reactive :as r]))

(def fs (js/require "fs"))

(defn log [& params]
  (.apply (.-log js/console) js/console (clj->js params))
  (last params))

(defn make-js-error [node-err]
  (log "node err" node-err)
  (if (instance? js/Error node-err)
    node-err
    (js/Error. (.-message node-err))))

(defn node->chan [f & args]
  (let [c (chan)
        callback (fn [err res]
                   (if-not err
                     (put! c (or res :done))
                     (put! c (make-js-error err)))
                   (close! c))
        args (conj (vec args) callback)]
    (apply f args)
    c))

(defn node-lift [f]
  (fn [& args]
    (go-catch
      (<? (apply node->chan f args)))))

(def rename (node-lift (.-rename fs)))
(def mkdir (node-lift (.-mkdir fs)))
(def lstat (node-lift (.-lstat fs)))
(def mkdir (node-lift (.-mkdir fs)))

(defn exists? [path]
  (let [out (chan)]
    (.exists fs path #(do (put! out %)
                          (close! out)))
    out))

(defn is-dir? [path]
  (go-catch
    (let [stat (<? (lstat path))]
      (.isDirectory stat))))

(defn is-file? [path]
  (go-catch
    (let [stat (<? (lstat path))]
      (.isFile stat))))

(defn mkdir-p [path]
  (let [c (->> (path/path-iterator path)
               (r/spool)
               (r/drop-while exists?))]
    (go-catch
      (loop []
        (when-let [v (<! c)]
          (<? (mkdir v))
          (recur)))
      :done)))

(defn match-extensions? [path extensions]
  (extensions (-> (path/extname path)
                  (subs 1))))

(defn read-dir [path]
  (let [fullpath (partial str path path/sep)]
    (go-catch
     (->> (<? (node->chan (.-readdir fs) path))
          array-seq
          (map fullpath)))))

(defn scan-paths
  ([paths] (scan-paths paths (chan)))
  ([input-paths out]
     (go
       (let [paths (atom (vec input-paths))]
         (while @paths
           (try
             (let [path (peek @paths)]
               (swap! paths next)
               (>! out path)
               (if (<? (is-dir? path)) (swap! paths into (<? (read-dir path)))))
             (catch js/Error e
               (>! out e))))
         (close! out)))
     out))

(defn scandir
  ([path] (scandir path (chan)))
  ([path out] (scan-paths [path] out)))
