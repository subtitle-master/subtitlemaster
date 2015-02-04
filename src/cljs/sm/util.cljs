(ns sm.util
  (:require [wilkerdev.util.nodejs :as node]
            [clojure.string :as str]))

(defn normalize-file-name [path]
  (-> (node/basename-without-extension path)
      (str/replace #"\." " ")))

(def regexp-full-size #"(?i)(.+)\sS(\d+)E(\d+)")

(def regexp-shorten #"(?i)(.+)\s(\d{1})(\d{2})")

(defn episode-info [path]
  (let [normalized-path (normalize-file-name path)]
    (when-let [[_ name season episode] (some #(re-find % normalized-path) [regexp-full-size regexp-shorten])]
      {:path    path
       :name    name
       :season  season
       :episode episode})))
