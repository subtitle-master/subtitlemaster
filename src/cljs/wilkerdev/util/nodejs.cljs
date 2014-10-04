(ns wilkerdev.util.nodejs
  (:require-macros [wilkerdev.util.macros :refer [<? go-catch]])
  (:require [cljs.core.async :refer [chan put! close!]]))

(defn make-js-error [node-err]
  (.log js/console "node err" node-err)
  (if (instance? js/Error node-err)
    node-err
    (js/Error. (.-message node-err))))

(defn node->chan [f & args]
  (let [c (chan 1)
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

(def fs (js/require "fs"))

(def rename (node-lift (.-rename fs)))
(def mkdir (node-lift (.-mkdir fs)))
(def lstat (node-lift (.-lstat fs)))
(def mkdir (node-lift (.-mkdir fs)))
(def fopen (node-lift (.-open fs)))
(def fread (node-lift (.-read fs)))
