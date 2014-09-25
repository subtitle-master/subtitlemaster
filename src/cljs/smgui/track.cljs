(ns smgui.track
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [clojure.string :as string]
            [smgui.settings]))

(declare make-js-map)

(def request-lib (js/require "request"))

(defn request
  ([url] (request url {}))
  ([url options]
    (let [options (-> (merge {:method "GET"
                              :url url} options)
                      clj->js)
          c (chan)]
      (request-lib options (fn [& args] (put! c args)))
      c)))

(def app-version (-> (js/require "./package.json") .-version))
(def platform-name (-> (js/require "os") .platform))

(defn track-info [type data]
  (merge {:v    1
          :t    type
          :tid  "UA-3833116-8"
          :cid  (smgui.settings/uuid)
          :aid  "com.subtitlemaster.nwapp"
          :aiid (str "com.nwgui." platform-name)
          :an   "SubtitleMaster"
          :av   app-version
          :cd1  (string/join (smgui.settings/languages) ",")}

         data))

(defn make-js-map
  "makes a javascript map from a clojure one"
  [cljmap]
  (let [out (js-obj)]
    (doseq [[key value] cljmap]
      (aset out (name key) value))
    out))

(defn track-request [type data]
  (let [options {:method "POST"
                 :form   (track-info type data)}]
    (request "http://www.google-analytics.com/collect" options)))

(defn screen [name]
  (track-request "screenview" {:cd name}))

(defn search [status]
  (track-request "event" {:ec "search" :ea status}))
