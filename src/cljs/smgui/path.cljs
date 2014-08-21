(ns smgui.path
  (:require [clojure.string :as str]))

(def nodepath (js/require "path"))

(def sep (.-sep nodepath))

(defn extname [path]
  (.extname nodepath path))

(defn basename [path]
  (.basename nodepath path))

(defn basename-without-extension [path]
  (.basename nodepath path (extname path)))

(defn path-iterator
  ([path] (path-iterator "" (remove str/blank? (str/split path sep))))
  ([current left]
     (if left
       (let [cur (str current sep (first left))]
         (cons cur (lazy-seq (path-iterator cur (next left)))))
       nil)))
