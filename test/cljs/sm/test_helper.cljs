(ns sm.test-helper
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sm.protocols :as sm]
            [cljs.core.async :as async]))

(def subdb-sandbox "http://sandbox.thesubdb.com/")
(def subdb-test-hash "edc1981d6459c6111fe36205b4aff6c2")

(defn fake-provider [res]
  (reify
    INamed
    (-name [_] "fake")

    sm/SearchProvider
    (search-subtitles [_ path _]
      (assert (= path "test/fixtures/famous.mkv"))
      (go res))))

(defn fake-linkable-provider []
  (reify
    INamed
    (-name [_] "fake2")

    sm/Linkable
    (-linkable-url [_] :url)))

(defn failing-provider []
  (reify
    sm/SearchProvider
    (search-subtitles [_ _ _]
      (assert false "this should not have been called"))))

(defn fake-subtitle [result lang]
  (reify
    sm/Subtitle
    (download-stream [_] result)
    (subtitle-language [_] lang)))

(defn upload-provider [status name]
  (reify
    INamed
    (-name [_] name)

    sm/UploadProvider
    (upload-subtitle [_ _ _] (go status))))

(defn mock
  ([] (mock nil))
  ([returning]
   (let [calls (atom [])
         mock-fn (fn [& args]
                   (swap! calls conj args)
                   returning)]
     [mock-fn calls])))
