(ns sm.core_test
  (:require-macros [wilkerdev.util.macros :refer [<? test dochan]]
                   [cljs.core.async.macros :refer [go]])
  (:require [sm.core :as sm]
            [cljs.core.async :refer [<!] :as async]
            [wilkerdev.util.nodejs :as node]
            [wilkerdev.util :as util]
            [sm.test_helper :as helper]))

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
    (let [res (<? (helper/reduce-cat (sm/process query)))]
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
    (let [res (-> (<? (helper/reduce-cat (sm/process query)))
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
        (let [res (-> (<? (helper/reduce-cat (sm/process query)))
                      (update-in [3 1] dissoc :sources)
                      (update-in [4 1] dissoc :source :subtitle))]
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
