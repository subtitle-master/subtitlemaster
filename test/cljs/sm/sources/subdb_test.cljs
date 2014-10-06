(ns sm.sources.subdb-test
  (:require-macros [wilkerdev.util.macros :refer [<? test]]
                   [cljs.core.async.macros :refer [go]])
  (:require [sm.sources.subdb :as subdb]
            [sm.protocols :as sm]
            [cljs.core.async :as async]
            [wilkerdev.util.nodejs :as node]
            [sm.test-helper :as helper]))

(test "encode subdb hash"
  (let [hash (<? (subdb/hash-file "test/fixtures/sample1.file"))]
    (assert (= hash "799fe265563e2150ee0e26f1ea0036c2"))))

(test "search subdb for valid hash"
  (binding [subdb/*subdb-endpoint* helper/subdb-sandbox]
    (let [response (<? (subdb/search-languages helper/subdb-test-hash))]
      (assert (= response [{:language "en" :count 1}
                           {:language "es" :count 1}
                           {:language "fr" :count 1}
                           {:language "it" :count 1}
                           {:language "pt" :count 2}])))))

(test "search subdb for invalid hash"
  (binding [subdb/*subdb-endpoint* helper/subdb-sandbox]
    (let [response (<? (subdb/search-languages "blabla"))]
      (assert (= [] response)))))

(test "download subtitle from subdb"
  (binding [subdb/*subdb-endpoint* helper/subdb-sandbox]
    (let [contents (<? (node/read-file "test/fixtures/subdb-download.srt" #js {:encoding "utf8"}))
          stream (subdb/download helper/subdb-test-hash "en")
          response (<? (node/stream->str stream))]
      (assert (= contents response)))))

(test "download invalid"
  (binding [subdb/*subdb-endpoint* helper/subdb-sandbox]
    (let [stream (subdb/download "blabla" "en")
          response (<? (async/reduce str "" (node/stream->chan stream)))]
      (assert (= "" response)))))

(test "upload subtitle"
  (binding [subdb/*subdb-endpoint* helper/subdb-sandbox]
    (let [stream (node/create-read-stream "test/fixtures/subdb-download.srt")
          response (<? (subdb/upload helper/subdb-test-hash stream))]
      (assert (= :duplicated response)))))

(test "subdb expand results"
  (let [result {:language "pt" :count 2}
        expected [{:language "pt" :hash "123" :version 0}
                  {:language "pt" :hash "123" :version 1}]]
    (assert (= (subdb/expand-result "123" result) expected))))

(test "subdb integration stack"
  (with-redefs [subdb/hash-file (fn [path]
                                (assert (= path "sample-path"))
                                (go helper/subdb-test-hash))
                subdb/search-languages (fn [hash]
                                            (assert (= hash helper/subdb-test-hash))
                                            (go [{:language "es" :count 2}
                                                 {:language "pt" :count 1}]))
                subdb/download (fn [hash lang version]
                                           (assert (= hash helper/subdb-test-hash))
                                           (assert (= lang "pt"))
                                           (assert (= version 0))
                                           "content")]
    (let [host (subdb/source)
          res (<? (sm/search-subtitles host "sample-path" ["en" "pb"]))]
      (assert (= "content" (sm/download-stream (first res)))))))
