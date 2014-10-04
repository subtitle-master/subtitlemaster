(ns sm.core-test
  (:require-macros [wilkerdev.util.macros :refer [<? test]])
  (:require [sm.core :as sm]))

(test "encode subdb hash"
  (let [hash (<? (sm/subdb-hash "test/fixtures/sample1.file"))]
    (assert (= hash "799fe265563e2150ee0e26f1ea0036c2"))))

(test "search subdb for valid hash"
  (let [response (<? (sm/subdb-search-languages "edc1981d6459c6111fe36205b4aff6c2"))]
    (assert (= response #{"en" "es" "fr" "it" "pt"}))))
