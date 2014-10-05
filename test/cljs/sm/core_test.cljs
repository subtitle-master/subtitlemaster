(ns sm.core_test
  (:require-macros [wilkerdev.util.macros :refer [<? test dochan]]
                   [cljs.core.async.macros :refer [go]])
  (:require [sm.core :as sm]
            [cljs.core.async :refer [<!] :as async]
            [wilkerdev.util.nodejs :as node]
            [wilkerdev.util :as util]
            [sm.test_helper :as helper]))

(test "encode subdb hash"
  (let [hash (<? (sm/subdb-hash "test/fixtures/sample1.file"))]
    (assert (= hash "799fe265563e2150ee0e26f1ea0036c2"))))

(test "search subdb for valid hash"
  (binding [sm/*subdb-endpoint* helper/subdb-sandbox]
    (let [response (<? (sm/subdb-search-languages helper/subdb-test-hash))]
      (assert (= response [{:language "en" :count 1}
                           {:language "es" :count 1}
                           {:language "fr" :count 1}
                           {:language "it" :count 1}
                           {:language "pt" :count 2}])))))

(test "search subdb for invalid hash"
  (binding [sm/*subdb-endpoint* helper/subdb-sandbox]
    (let [response (<? (sm/subdb-search-languages "blabla"))]
      (assert (= [] response)))))

(test "download subtitle from subdb"
  (binding [sm/*subdb-endpoint* helper/subdb-sandbox]
    (let [contents (<? (node/read-file "test/fixtures/subdb-download.srt" #js {:encoding "utf8"}))
          stream (sm/subdb-download-stream helper/subdb-test-hash "en")
          response (<? (node/stream->str stream))]
      (assert (= contents response)))))

(test "download invalid"
  (binding [sm/*subdb-endpoint* helper/subdb-sandbox]
    (let [stream (sm/subdb-download-stream "blabla" "en")
          response (<? (async/reduce str "" (node/stream->chan stream)))]
      (assert (= "" response)))))

(test "upload subtitle"
  (binding [sm/*subdb-endpoint* helper/subdb-sandbox]
    (let [stream (node/create-read-stream "test/fixtures/subdb-download.srt")
          response (<? (sm/subdb-upload helper/subdb-test-hash stream))]
      (assert (= :duplicated response)))))

(test "open subtitles hash"
  (let [[hash size] (<? (sm/opensub-hash "test/fixtures/breakdance.avi"))]
    (assert (= "8e245d9679d31e12" hash))
    (assert (= 12909756 size))))

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
                                (go helper/subdb-test-hash))
                sm/subdb-search-languages (fn [hash]
                                            (assert (= hash helper/subdb-test-hash))
                                            (go [{:language "es" :count 2}
                                                 {:language "pt" :count 1}]))
                sm/subdb-download-stream (fn [hash lang version]
                                           (assert (= hash helper/subdb-test-hash))
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

(test "open subtitles subtitle language"
  (let [subtitle (sm/->OpenSubtitlesSubtitle {:sub-language-id "eng"})]
    (assert (= "en" (sm/subtitle-language subtitle)))))

(test "reading subtitle info"
  (let [info (<? (sm/subtitle-info "test/fixtures/clip1.mkv"))]
    (assert (= info {:path      "test/fixtures/clip1.mkv"
                     :basepath  "test/fixtures/clip1"
                     :subtitles #{}})))
  (let [info (<? (sm/subtitle-info "test/fixtures/clip-subtitled.mkv"))]
    (assert (= info {:path      "test/fixtures/clip-subtitled.mkv"
                     :basepath  "test/fixtures/clip-subtitled"
                     :subtitles #{:plain}})))
  (let [info (<? (sm/subtitle-info "test/fixtures/famous.mkv"))]
    (assert (= info {:path      "test/fixtures/famous.mkv"
                     :basepath  "test/fixtures/famous"
                     :subtitles #{"en" "pt"}}))))

(test "subtitle language path"
  (assert (= "path.srt" (sm/subtitle-target-path "path" :plain)))
  (assert (= "path.en.srt" (sm/subtitle-target-path "path" "en"))))

(test "finding a single item"
  (let [sources [(helper/fake-provider []) (helper/fake-provider [:subtitle]) (helper/failing-provider)]
        sub (<? (sm/find-first {:sources   sources
                                :path      "test/fixtures/famous.mkv"
                                :languages ["pb"]}))]
    (assert (= {:subtitle :subtitle
                :source   (nth sources 1)} sub))))

(test "return new when nothing is found"
  (let [sources [(helper/fake-provider []) (helper/fake-provider [])]
        sub (<? (sm/find-first {:sources   sources
                                :path      "test/fixtures/famous.mkv"
                                :languages ["pb"]}))]
    (assert (= nil sub))))

(test "search download operation - unchanged"
  (let [query {:sources   []
               :path      "test/fixtures/famous.mkv"
               :languages ["pt"]}]
    (let [res (<? (helper/reduce-cat (sm/search-download query)))]
      (assert (= res [[:init]
                      [:info {:path      "test/fixtures/famous.mkv"
                              :basepath  "test/fixtures/famous"
                              :subtitles #{"en" "pt"}}]
                      [:view-path "test/fixtures/famous.mkv"]
                      [:unchanged]])))))

(test "search download operation - not found"
  (let [query {:sources   [(helper/fake-provider [])]
               :path      "test/fixtures/famous.mkv"
               :languages ["pb"]}]
    (let [res (-> (<? (helper/reduce-cat (sm/search-download query)))
                  (update-in [3 1] dissoc :sources))]
      (assert (= res
                 [[:init]
                  [:info {:path      "test/fixtures/famous.mkv"
                          :basepath  "test/fixtures/famous"
                          :subtitles #{"en" "pt"}}]
                  [:view-path "test/fixtures/famous.mkv"]
                  [:search {:path "test/fixtures/famous.mkv" :languages ["pb"]}]
                  [:not-found]])))))

(test "search download operation - downloaded"
  (let [called (atom false)]
    (with-redefs [node/save-stream-to (fn [& args]
                                        (reset! called args)
                                        (go nil))]
      (let [query {:sources   [(helper/fake-provider [(helper/fake-subtitle :stream "pb")])]
                   :path      "test/fixtures/famous.mkv"
                   :languages ["pb"]}]
        (let [res (-> (<? (helper/reduce-cat (sm/search-download query)))
                      (update-in [3 1] dissoc :sources)
                      (update-in [4 1] dissoc :source :subtitle))]
          (.log js/console (pr-str res))
          (assert (= res
                     [[:init]
                      [:info {:path      "test/fixtures/famous.mkv"
                              :basepath  "test/fixtures/famous"
                              :subtitles #{"en" "pt"}}]
                      [:view-path "test/fixtures/famous.mkv"]
                      [:search {:path "test/fixtures/famous.mkv" :languages ["pb"]}]
                      [:download {:target "test/fixtures/famous.pb.srt"}]
                      [:downloaded]]))
          (assert (= @called [:stream "test/fixtures/famous.pb.srt"])))))))
