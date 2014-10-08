(ns sm.core-test
  (:require-macros [wilkerdev.util.macros :refer [<? test dochan]]
                   [cljs.core.async.macros :refer [go]])
  (:require [sm.core :as sm]
            [sm.protocols :as prot]
            [cljs.core.async :refer [<!] :as async]
            [wilkerdev.util.nodejs :as node]
            [wilkerdev.util :as util]
            [sm.test-helper :as helper]))

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

(test "simple md5 hash"
  (assert (= (<? (node/md5-file "test/fixtures/dexter.mp4")) "5bb798f7d3ed095492dca31bcf0155fd")))

(test "subtitle language path"
  (assert (= "path.srt" (sm/subtitle-target-path "path" :plain)))
  (assert (= "path.en.srt" (sm/subtitle-target-path "path" "en"))))

(test "source info"
  (let [s (helper/fake-provider [])]
    (assert (= {:source      s
                :source-name "fake"} (sm/source-info s)))))

(test "finding a single item"
  (let [sources [(helper/fake-provider []) (helper/fake-provider [:subtitle]) (helper/failing-provider)]
        sub (<? (sm/find-first {:sources   sources
                                :path      "test/fixtures/famous.mkv"
                                :languages ["pb"]}))]
    (assert (= {:subtitle    :subtitle
                :source      (nth sources 1)
                :source-name "fake"} sub))))

(test "finding all options"
  (let [sa (helper/fake-provider [:one :two])
        sb (helper/fake-provider [:three])
        sources [sa sb]
        search (sm/find-all {:sources   sources
                             :path      "test/fixtures/famous.mkv"
                             :languages ["pb"]})]
    (assert (= #{{:source      sa
                  :source-name "fake"
                  :subtitles   [:one :two]}
                 {:source      sb
                  :source-name "fake"
                  :subtitles   [:three]}} (<? (async/into #{} search))))))

(test "return new when nothing is found"
  (let [sources [(helper/fake-provider []) (helper/fake-provider [])]
        sub (<? (sm/find-first {:sources   sources
                                :path      "test/fixtures/famous.mkv"
                                :languages ["pb"]}))]
    (assert (= nil sub))))

(test "in memory cache"
  (let [cache (sm/in-memory-cache)]
    (assert (false? (prot/cache-exists? cache "abc")))
    (prot/cache-store! cache "abc")
    (assert (true? (prot/cache-exists? cache "abc")))))

(test "process - unchanged"
  (let [query {:sources   []
               :path      "test/fixtures/famous.mkv"
               :languages ["pt"]}]
    (let [res (<? (async/into [] (sm/process query)))]
      (assert (= res [[:init]
                      [:info {:path      "test/fixtures/famous.mkv"
                              :basepath  "test/fixtures/famous"
                              :subtitles #{"en" "pt"}}]
                      [:view-path "test/fixtures/famous.mkv"]
                      [:unchanged]])))))

(test "process - upload"
  (let [cache (atom {})
        query {:sources   [(helper/upload-provider :uploaded "provider")]
               :path      "test/fixtures/breakdance.avi"
               :languages ["en"]
               :cache     (sm/in-memory-cache cache)}]
    (let [res (<? (async/into [] (sm/process query)))]
      (assert (= @cache {"559075d64f311fba4abc08a43b1eff7e-711ea91b78058e01151e79f783dc6955-provider" true}))
      (assert (= res [[:init]
                      [:info {:path      "test/fixtures/breakdance.avi"
                              :basepath  "test/fixtures/breakdance"
                              :subtitles #{"en"}}]
                      [:view-path "test/fixtures/breakdance.avi"]
                      [:upload "test/fixtures/breakdance.en.srt"]
                      [:uploaded :uploaded]
                      [:unchanged]])))))

(test "process - not found"
  (let [query {:sources   [(helper/fake-provider [])]
               :path      "test/fixtures/famous.mkv"
               :languages ["pb"]}]
    (let [res (-> (<? (async/into [] (sm/process query)))
                  (update-in [3 1] dissoc :sources))]
      (assert (= res
                 [[:init]
                  [:info {:path      "test/fixtures/famous.mkv"
                          :basepath  "test/fixtures/famous"
                          :subtitles #{"en" "pt"}}]
                  [:view-path "test/fixtures/famous.mkv"]
                  [:search {:path "test/fixtures/famous.mkv" :languages ["pb"]}]
                  [:not-found]])))))

(test "process - downloaded"
  (let [called (atom false)]
    (with-redefs [node/save-stream-to (fn [& args]
                                        (reset! called args)
                                        (go nil))]
      (let [query {:sources   [(helper/fake-provider [(helper/fake-subtitle :stream "pb")])]
                   :path      "test/fixtures/famous.mkv"
                   :languages ["pb"]}]
        (let [res (-> (<? (async/into [] (sm/process query)))
                      (update-in [3 1] dissoc :sources)
                      (update-in [4 1] dissoc :source :subtitle))]
          (assert (= res
                     [[:init]
                      [:info {:path      "test/fixtures/famous.mkv"
                              :basepath  "test/fixtures/famous"
                              :subtitles #{"en" "pt"}}]
                      [:view-path "test/fixtures/famous.mkv"]
                      [:search {:path "test/fixtures/famous.mkv" :languages ["pb"]}]
                      [:download {:source-name "fake" :target "test/fixtures/famous.pb.srt"}]
                      [:downloaded]]))
          (assert (= @called [:stream "test/fixtures/famous.pb.srt"])))))))
