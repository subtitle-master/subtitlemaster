(ns smgui.ttiual
  (:require (clojure.string :as str)))

(defn in? [seq elm]
  (some #(= elm %) seq))

(defn not-in? [seq elm]
  (not (in? seq elm)))

(defn class-set [map]
  (->> map
       (filter (fn [[_ b]] (true? b)))
       keys
       (str/join " ")))
