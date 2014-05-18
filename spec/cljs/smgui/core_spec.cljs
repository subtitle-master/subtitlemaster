(ns smgui.core-spec
  (:require-macros [speclj.core :refer [describe it should should-not should=]])
  (:require [speclj.core]))

(defn- swap-indexes [seq a b]
  (-> seq
      (assoc b (nth seq a))
      (assoc a (nth seq b))))

(defn move-up-indexes [input indexes]
  (reduce
    (fn [list index]
      (let [next (dec index)]
        (if (and (>= next 0))
          (swap-indexes list index next)
          list)))
    input
    indexes))

(defn move-down-indexes [input indexes]
  (reduce
    (fn [list index]
      (let [next (inc index)]
        (if (and (< next (count list)))
          (swap-indexes list index next)
          list)))
    input
    (reverse indexes)))

(describe "move-up"
  (it "returns same array for blank input"
    (should= '[a b] (move-up-indexes '[a b] [])))
  (it "doesn't move from zero"
    (should= '[a b] (move-up-indexes '[a b] [0])))
  (it "moves up"
    (should= '[b a] (move-up-indexes '[a b] [1])))
  (it "moves up from a third place"
    (should= '[a c b] (move-up-indexes '[a b c] [0 2])))
  (it "moves up from a bottom"
    (should= '[b c a] (move-up-indexes '[a b c] [1 2]))))

(describe "move-down"
  (it "returns same array for blank input"
    (should= '[a b] (move-down-indexes '[a b] [])))
  (it "doesn't move from last"
    (should= '[a b] (move-down-indexes '[a b] [1])))
  (it "moves down"
    (should= '[b a] (move-down-indexes '[a b] [0])))
  (it "moves down from a third place"
    (should= '[b a c] (move-down-indexes '[a b c] [0 2])))
  (it "correct moves down multiple from top"
      (should= '[c a b] (move-down-indexes '[a b c] [0 1]))))
