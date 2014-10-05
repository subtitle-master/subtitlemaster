(ns sm.core-test
  (:require-macros [wilkerdev.util.macros :refer [<? test]]
                   [cljs.core.async.macros :refer [go]])
  (:require [sm.core :as sm]
            [cljs.core.async :as async]
            [wilkerdev.util.nodejs :as node]
            [wilkerdev.util :as util]))

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
          stream (sm/subdb-download subdb-test-hash "en")
          response (<? (async/reduce str "" (node/stream->chan stream)))]
      (assert (= contents response)))))

(test "download invalid"
  (binding [sm/*subdb-endpoint* subdb-sandbox]
    (let [stream (sm/subdb-download "blabla" "en")
          response (<? (async/reduce str "" (node/stream->chan stream)))]
      (assert (= "" response)))))

(test "upload subtitle"
  (binding [sm/*subdb-endpoint* subdb-sandbox]
    (let [stream (node/create-read-stream "test/fixtures/subdb-download.srt")
          response (<? (sm/subdb-upload subdb-test-hash stream))]
      (assert (= :duplicated response)))))

(test "open subtitles hash"
  (let [[hash size] (<? (sm/opensub-hash "test/fixtures/breakdance.avi"))]
    (assert (= "8e245d9679d31e12" hash))
    (assert (= 12909756 size))))

(def osres (atom nil))

(test "open subtitles search"
  (let [conn (sm/opensub-client)
        token (<? (sm/opensub-auth conn))
        query [{:sublanguageid "pob,eng"
                :moviehash     "cf2490e0d1ecddb6"
                :moviebytesize 833134592}]
        res (<? (sm/opensub-search conn token query))]
    (assert (> (count res) 0))))

(test "open subtitles download"
  (let [entry {:sub-download-link "http://dl.opensubtitles.org/en/download/filead/1118.gz"}
        stream (sm/opensub-download-stream entry)
        response (<? (async/reduce str "" (node/stream->chan stream)))]
    (assert (> (count response) 3000))))

(test "subdb expand results"
  (let [result {:language "pt" :count 2}
        expected [{:language "pt" :hash "123" :version 0}
                  {:language "pt" :hash "123" :version 1}]]
    (assert (= (sm/subdb-expand-result "123" result) expected))))

(test "subdb integration stack"
  (with-redefs [sm/subdb-hash (fn [path]
                                (assert (= path "sample-path"))
                                (go subdb-test-hash))
                sm/subdb-search-languages (fn [hash]
                                            (assert (= hash subdb-test-hash))
                                            (go [{:language "es" :count 2}
                                                 {:language "pt" :count 1}]))
                sm/subdb-download (fn [hash lang version]
                                    (assert (= hash subdb-test-hash))
                                    (assert (= lang "pt"))
                                    (assert (= version 0))
                                    "content")]
    (let [host (sm/->SubDBSource)
          res (<? (sm/search-subtitles host "sample-path" ["en" "pb"]))]
      (assert (= "content" (sm/download-stream (first res)))))))

(test "open subtitles integration stack"
  (with-redefs [sm/opensub-hash (fn [path]
                                  (assert (= path "sample-path"))
                                  (go ["abc" 123]))
                sm/opensub-client (constantly :client)
                sm/opensub-auth (fn [client]
                                  (assert (= :client client))
                                  (go :auth))
                sm/opensub-search (fn [client auth query]
                                    (assert (= client :client))
                                    (assert (= auth :auth))
                                    (assert (= query [{:sublanguageid "eng,por"
                                                       :moviehash     "abc"
                                                       :moviebytesize 123}]))
                                    (go [{:sub-download-link "download-url"}]))
                sm/opensub-download-stream (fn [info]
                                             (assert (= info {:sub-download-link "download-url"}))
                                             "content")]
    (let [host (<? (sm/opensub-source))
          res (<? (sm/search-subtitles host "sample-path" ["en" "pt"]))]
      (assert (= "content" (sm/download-stream (first res)))))))
