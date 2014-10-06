(ns wilkerdev.util.reactive-test
  (:require-macros [wilkerdev.util.macros :refer [<? test dochan]]
                   [cljs.core.async.macros :refer [go]])
  (:require [wilkerdev.util.reactive :as r]
            [cljs.core.async :refer [<! timeout]]))

(defn counting [f]
  (let [counter (atom 0)]
    [(fn [& args]
       (swap! counter inc)
       (apply f args))
     counter]))

(test "async memorize"
  (let [[f counter] (counting (fn [x] (go x)))
        memoized (r/memoize-async f)]
    (assert (= 1 (<! (memoized 1))))
    (assert (= 1 (<! (memoized 1))))
    (assert (= 5 (<! (memoized 5))))
    (assert (= 5 (<! (memoized 5))))
    (assert (= 5 (<! (memoized 5))))
    (assert (= 2 @counter))))
