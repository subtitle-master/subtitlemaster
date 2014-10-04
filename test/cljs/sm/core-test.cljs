(ns sm.core-test
  (:require-macros [wilkerdev.util.macros :refer [<? test]])
  (:require [sm.core :as sm]))

(test "encode subdb hash"
  (let [hash (<? (sm/subdb-hash "test/fixtures/sample1.file"))]
    (assert (= hash "799fe265563e2150ee0e26f1ea0036c2"))))
