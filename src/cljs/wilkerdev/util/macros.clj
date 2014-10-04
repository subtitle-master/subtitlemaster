(ns wilkerdev.util.macros
  (:refer-clojure :exclude [test]))

(defmacro dochan [[binding chan] & body]
  `(let [chan# ~chan]
     (cljs.core.async.macros/go
       (loop []
         (if-let [~binding (cljs.core.async/<! chan#)]
           (do
             ~@body
             (recur))
           :done)))))

(defmacro <? [ch]
  `(wilkerdev.util.reactive/throw-err (cljs.core.async/<! ~ch)))

(defmacro go-catch [& body]
  `(cljs.core.async.macros/go
     (try
       ~@body
       (catch js/Error e e))))

(defmacro <!expand [value]
  `(cljs.core.async/<! (wilkerdev.util.reactive/expand-value ~value)))

(defmacro test [title & body]
  `(cljs.core.async.macros/go
     (try
       ~@body
       (.log js/console "Passed:" ~title)
       (catch js/Error e#
         (.log js/console "Failed: " ~title ":" e#)))))
