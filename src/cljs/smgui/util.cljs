(ns smgui.util
  (:require (clojure.string :as str)))

(def uuid-lib (js/require "node-uuid"))

(defn uuid-v4 [] (.v4 uuid-lib))

(defn in? [seq elm]
  (some #(= elm %) seq))

(defn not-in? [seq elm]
  (not (in? seq elm)))

(defn class-set [map]
  (->> map
       (filter (fn [[_ b]] (true? b)))
       keys
       (str/join " ")))

(def fs (js/require "fs"))

(defn copy-file [source target]
  (let [input (.createReadStream fs source)
        output (.createWriteStream fs target)]
    (.pipe input output)))
