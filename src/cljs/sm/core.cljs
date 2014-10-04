(ns sm.core
  (:require-macros [wilkerdev.util.macros :refer [<? go-catch]])
  (:require [wilkerdev.util.nodejs :refer [lstat fopen fread]]))

(def crypto (js/require "crypto"))

(def chunk-size (* 64 1024))

(defn subdb-hash [path]
  (go-catch
    (let [size (.-size (<? (lstat path)))
          end-start (- size chunk-size)
          _ (assert (>= end-start 0) (str "File size must be at least " chunk-size " bytes"))
          file (<? (fopen path "r"))
          buffer (js/Buffer. (* 2 chunk-size))]

      (<? (fread file buffer 0 chunk-size 0))
      (<? (fread file buffer chunk-size chunk-size end-start))

      (let [sum (.createHash crypto "md5")]
        (.update sum buffer)
        (.digest sum "hex")))))
