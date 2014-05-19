(ns smgui.track
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [chan put!]]
            [smgui.uuid :as uuid]
            [clojure.string :as string]))

(def request-lib (.require js/window "request"))

(def app-version (-> (.require js/window "./package.json") .-version))
(def platform-name (-> (.require js/window "os") .platform))

(defn track-info [type data]
  (merge {:v    1
          :t    type
          :tid  "UA-3833116-8"
          :cid  (smgui.settings/uuid)
          :aid  "com.subtitlemaster.nwapp"
          :aiid (str "com.nwgui." platform-name)
          :an   "SubtitleMaster"
          :av   app-version
          :cd1  (string/join (smgui.settings/languages) ",")} data))

(defn make-js-map
  "makes a javascript map from a clojure one"
  [cljmap]
  (let [out (js-obj)]
    (doall (map #(aset out (name (first %)) (second %)) cljmap))
    out))

(defn track-request [type data]
  (let [c (chan)
        form (-> (track-info type data) make-js-map)
        request #js {:url    "http://www.google-analytics.com/collect"
                     :method "POST"
                     :form form}]
    (request-lib request
                 #(put! c %2))
    c))

(defn screen [name]
  (track-request "screenview" {:cd name}))

(defn search [status]
  (track-request "event" {:ec "search" :ea status}))
