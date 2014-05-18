(ns smgui.ttiual-spec
  (:require-macros [speclj.core :refer [describe it should should-not should=]])
  (:require [speclj.core]
            [smgui.ttiual :refer [in? not-in? class-set]]))

(describe "in?"
          (it "checks if element is included on sequence"
              (should-not (in? [1 2 3] 4))
              (should (in? [1 2 3] 1))
              (should (in? [1 2 3] 2))
              (should (in? [1 2 3] 3))))

(describe "not-in?"
          (it "checks if element is not included on sequence"
              (should (not-in? [1 2 3] 4))
              (should-not (not-in? [1 2 3] 1))
              (should-not (not-in? [1 2 3] 2))
              (should-not (not-in? [1 2 3] 3))))

(describe "class-set"
  (it "filters the keys by their values"
    (should= "valid" (class-set {"valid" true "invalid" false}))
    (should= "one two" (class-set {"one" true "two" true}))))
