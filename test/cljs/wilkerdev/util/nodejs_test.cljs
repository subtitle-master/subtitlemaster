(ns wilkerdev.util.nodejs-test
  (:require-macros [wilkerdev.util.macros :refer [<? test]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async]
            [wilkerdev.util.nodejs :as node]
            [sm.test-helper :as helper]))

(test "stream->buffer"
  (let [path "test/fixtures/breakdance.en.srt"]
    (let [stream (node/create-read-stream path)]
      (assert (= 93905 (-> stream
                           node/stream->buffer
                           <?
                           .-length))))))
