(ns sm.util
  (:require [wilkerdev.util.nodejs :as node]
            [clojure.string :as str]))

(defn normalize-file-name [path]
  (-> (node/basename-without-extension path)
      (str/replace #"\." " ")))

(defn episode-info [path]
  (if-let [[_ name season episode] (re-find #"(?i)(.+)\sS(\d+)E(\d+)" (normalize-file-name path))]
    {:path path
     :name name
     :season season
     :episode episode}))
