(ns smgui.engine
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [smgui.track :as track]
            [sm.protocols :refer [CacheStore]]))

(defn cache-key [hash] (str "share-cache-" hash))

(defn local-storage-get [key] (aget (-> js/window .-localStorage) key))
(defn local-storage-set! [key value] (aset (-> js/window .-localStorage) key value))

(defn local-storage-cache []
  (reify
    CacheStore
    (cache-exists? [_ hash]
      (local-storage-get (cache-key hash)))
    (cache-store! [_ hash]
      (.log js/console "storing cache" hash)
      (local-storage-set! (cache-key hash) true))))
