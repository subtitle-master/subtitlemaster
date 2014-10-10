(ns smgui.engine
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [smgui.track :as track]
            [sm.protocols :refer [CacheStore]]
            [cljs.reader :refer [read-string]]))

(defn cache-key [hash] (str "share-cache-" hash))

(defn local-storage-get [key]
  (try
    (read-string (aget (-> js/window .-localStorage) (name key)))
    (catch js/Error _
      nil)))

(defn local-storage-set! [key value]
  (aset (-> js/window .-localStorage) (name key) (pr-str value)))

(defn local-storage-cache []
  (reify
    CacheStore
    (cache-exists? [_ hash]
      (local-storage-get (cache-key hash)))
    (cache-store! [_ hash]
      (local-storage-set! (cache-key hash) true))))
