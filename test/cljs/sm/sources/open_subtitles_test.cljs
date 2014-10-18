(ns sm.sources.open-subtitles-test
  (:require-macros [wilkerdev.util.macros :refer [<? test]]
                   [cljs.core.async.macros :refer [go]])
  (:require [sm.sources.open-subtitles :as os]
            [sm.protocols :as sm]
            [cljs.core.async :as async]
            [wilkerdev.util.nodejs :as node]
            [sm.test-helper :as helper]))

(test "open subtitles hash"
  (let [[hash size] (<? (os/hash-file "test/fixtures/breakdance.avi"))]
    (assert (= "8e245d9679d31e12" hash))
    (assert (= 12909756 size))))

(test "open subtitles search"
  (let [conn (os/client)
        token (<? (os/auth conn))
        query [{:sublanguageid "pob,eng"
                :moviehash     "cf2490e0d1ecddb6"
                :moviebytesize 833134592}]
        res (<? (os/search conn token query))]
    (.log js/console (clj->js res))
    (assert (> (count res) 0))))

(test "build query search"
  (assert (= (<? (os/build-query "test/fixtures/breakdance.avi" ["pb"]))
             [{:moviebytesize 12909756
               :sublanguageid "pob"
               :moviehash     "8e245d9679d31e12"}
              {:sublanguageid "pob"
               :tag           "breakdance.avi"}])))

(test "build tv show query"
  (assert (= (<? (os/build-query "test/fixtures/Some.Show.Name.S01E03.720p.HDTV.mkv" ["pb"]))
             [{:moviebytesize 93905
               :sublanguageid "pob"
               :moviehash     "39e0e54a3a1cffc9"}
              {:sublanguageid "pob"
               :season        1
               :episode       3
               :query         "Some Show Name"}
              {:sublanguageid "pob"
               :tag           "Some.Show.Name.S01E03.720p.HDTV.mkv"}])))

(test "open subtitles download"
  (let [entry {:sub-download-link "http://dl.opensubtitles.org/en/download/filead/1118.gz"}
        stream (os/download entry)
        response (<? (async/reduce str "" (node/stream->chan stream)))]
    (assert (> (count response) 3000))))

(test "open subtitles integration stack"
  (with-redefs [os/hash-file (fn [path]
                               (assert (= path "sample-path"))
                               (go ["abc" 123]))
                os/client (constantly :client)
                os/auth (fn [client]
                          (assert (= :client client))
                          (go :auth))
                os/search (fn [client auth query]
                            (assert (= client :client))
                            (assert (= auth :auth))
                            (assert (= query [{:sublanguageid "eng,por"
                                               :moviehash     "abc"
                                               :moviebytesize 123}]))
                            (go [{:sub-download-link "download-url"}]))
                os/download (fn [info]
                              (assert (= info {:sub-download-link "download-url"}))
                              "content")]
    (let [host (os/source)
          res (<? (sm/search-subtitles host "sample-path" ["en" "pt"]))]
      (assert (= "content" (sm/download-stream (first res)))))))

(test "open subtitles subtitle language"
  (let [subtitle (os/->OpenSubtitlesSubtitle {:sub-language-id "eng"})]
    (assert (= "en" (sm/subtitle-language subtitle)))))

(test "upload"
  (let [conn (os/client)
        token (<? (os/auth conn))
        query {:conn     conn
               :auth     token
               :path     "test/fixtures/breakdance.avi"
               :sub-path "test/fixtures/breakdance.en.srt"}]
    (.log js/console (clj->js (<? (os/upload query))))))
