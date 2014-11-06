(ns releaser.core-test
  (:require [clojure.test :refer :all]
            [releaser.core :refer :all]))

(deftest test-gh-upload-release
  (println "GH Upload"))

(deftest test-generate-latest-json
  (let [build-info (-> (slurp "tmp/nw-build/build-info.edn")
                      read-string)
        release-info (-> (slurp "tmp/github-release-info.edn")
                        read-string)]
    (clojure.pprint/pprint (generate-latest-info build-info release-info))))
