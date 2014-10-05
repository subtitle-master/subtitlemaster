(ns sm.sources.open-subtitles-test
  (:require-macros [wilkerdev.util.macros :refer [<? test]]
                   [cljs.core.async.macros :refer [go]])
  (:require [sm.sources.open-subtitles :as os]
            [sm.protocols :as sm]
            [cljs.core.async :as async]
            [wilkerdev.util.nodejs :as node]
            [sm.test_helper :as helper]))

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
    (assert (> (count res) 0))))

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
    (let [host (<? (os/source))
          res (<? (sm/search-subtitles host "sample-path" ["en" "pt"]))]
      (assert (= "content" (sm/download-stream (first res)))))))

(test "open subtitles subtitle language"
  (let [subtitle (os/->OpenSubtitlesSubtitle {:sub-language-id "eng"})]
    (assert (= "en" (sm/subtitle-language subtitle)))))
