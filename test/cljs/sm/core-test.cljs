(ns sm.core-test
  (:require-macros [wilkerdev.util.macros :refer [<? test]])
  (:require [sm.core :as sm]
            [wilkerdev.util.nodejs :as node]))

(def subdb-sandbox "http://sandbox.thesubdb.com/")
(def subdb-test-hash "edc1981d6459c6111fe36205b4aff6c2")

(test "encode subdb hash"
  (let [hash (<? (sm/subdb-hash "test/fixtures/sample1.file"))]
    (assert (= hash "799fe265563e2150ee0e26f1ea0036c2"))))

(test "search subdb for valid hash"
  (binding [sm/*subdb-endpoint* subdb-sandbox]
    (let [response (<? (sm/subdb-search-languages subdb-test-hash))]
      (assert (= response [{:language "en" :count 1}
                           {:language "es" :count 1}
                           {:language "fr" :count 1}
                           {:language "it" :count 1}
                           {:language "pt" :count 2}])))))

(test "search subdb for invalid hash"
  (binding [sm/*subdb-endpoint* subdb-sandbox]
    (let [response (<? (sm/subdb-search-languages "blabla"))]
      (assert (nil? response)))))

(test "download subtitle from subdb"
  (binding [sm/*subdb-endpoint* subdb-sandbox]
    (let [contents (<? (node/read-file "test/fixtures/subdb-download.srt" #js {:encoding "utf8"}))
          response  (<? (sm/subdb-download subdb-test-hash "en"))]
      (assert (= contents response)))))

(test "download invalid"
  (binding [sm/*subdb-endpoint* subdb-sandbox]
    (let [contents (<? (node/read-file "test/fixtures/subdb-download.srt" #js {:encoding "utf8"}))
          response  (<? (sm/subdb-download subdb-test-hash "en"))]
      (assert (= contents response)))))
