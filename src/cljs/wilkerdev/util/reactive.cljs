(ns wilkerdev.util.reactive
  (:refer-clojure :exclude [map mapcat filter remove distinct concat take-while drop-while complement keep reduce])
  (:require [goog.net.Jsonp]
            [goog.Uri]
            [goog.events :as events]
            [cljs.core.async :refer [>! <! chan put! close! timeout alts!] :as async])
  (:require-macros [cljs.core.async.macros :refer [go alt! go-loop]]
                   [wilkerdev.util.macros :refer [dochan go-catch <? <!expand]])
  (:import goog.events.EventType))

(defn browser-log [& params]
  (.apply (.-log js/console) js/console (clj->js params))
    (last params))

(def js-error? (partial instance? js/Error))

(def remove-js-errors (cljs.core/remove js-error?))

(defn throw-err [e]
  (when (js-error? e) (throw e))
  e)

(defn log
  ([in] (log nil in))
  ([prefix in]
    (let [out (chan)]
      (dochan [value in]
        (if prefix (browser-log prefix value)
                   (browser-log value))
        (>! out value))
      out)))

(defn listen
  ([el evt] (listen el evt (chan)))
  ([el evt c]
   (events/listen el evt #(put! c %))
   c))

(defn expand-value [c]
  (let [out (chan)]
    (go (loop [v c]
          (if v
            (if (satisfies? cljs.core.async.impl.protocols/ReadPort v)
              (recur (<! v))
              (do
                (>! out v)
                (close! out)))
            (close! out))))
    out))

(defn map [f in]
  (let [out (chan)]
    (go (loop []
          (if-let [x (<! in)]
            (do (>! out (f x))
              (recur))
            (close! out))))
    out))

(defn keep [f in]
  (let [out (chan)]
    (go (loop []
          (if-let [x (<! in)]
            (do
              (if-let [res (f x)]
                (>! out res))
              (recur))
            (close! out))))
    out))

(defn mapchan
  ([in] (mapchan nil in))
  ([f in]
    (let [out (chan)]
      (go (loop []
            (if-let [x (<! in)]
              (do
                (dochan [v (if f (f x) x)] (>! out v))
                (recur))
              (close! out))))
      out)))

(defn mapcat
  ([in] (mapcat nil in))
  ([f in]
    (let [out (chan)]
      (go (loop []
            (if-let [x (<! in)]
              (do
                (loop [y (if f (f x) x)]
                  (when y
                    (>! out (first y))
                    (recur (next y))))
                (recur))
              (close! out))))
      out)))

(defn filter [pred in]
  (let [out (chan)]
    (go (loop []
          (if-let [x (<! in)]
            (do (when (<!expand (pred x)) (>! out x))
              (recur))
            (close! out))))
    out))

(defn remove [f in]
  (let [out (chan)]
    (go (loop []
          (if-let [v (<! in)]
            (do (when-not (<!expand (f v)) (>! out v))
              (recur))
            (close! out))))
    out))

(defn spool [xs]
  (let [out (chan)]
    (go (loop [xs (seq xs)]
          (if xs
            (do (>! out (first xs))
              (recur (next xs)))
            (close! out))))
    out))

(defn split
  ([pred in] (split pred in [(chan) (chan)]))
  ([pred in [out1 out2]]
    (go (loop []
          (if-let [v (<! in)]
            (if (pred v)
              (do (>! out1 v)
                (recur))
              (do (>! out2 v)
                (recur))))))
    [out1 out2]))

(defn concat [xs in]
  (let [out (chan)]
    (go (loop [xs (seq xs)]
          (if xs
            (do (>! out (first xs))
              (recur (next xs)))
            (if-let [x (<! in)]
              (do (>! out x)
                (recur xs))
              (close! out)))))
    out))

(defn distinct [in]
  (let [out (chan)]
    (go (loop [last nil]
          (if-let [x (<! in)]
            (do (when (not= x last) (>! out x))
              (recur x))
            (close! out))))
    out))

(defn fan-in
  ([ins] (fan-in ins (chan)))
  ([ins out]
    (go (loop [ins (vec ins)]
          (when (> (count ins) 0)
            (let [[x in] (alts! ins)]
              (when x
                (>! out x)
                (recur ins))
              (recur (vec (disj (set ins) in))))))
        (close! out))
    out))

(defn take-until
  ([pred-sentinel in] (take-until pred-sentinel in (chan)))
  ([pred-sentinel in out]
    (go (loop []
          (if-let [v (<! in)]
            (do
              (>! out v)
              (if-not (<!expand (pred-sentinel v))
                (recur)
                (close! out)))
            (close! out))))
    out))

(defn drop-while
  ([sentinel in] (drop-while sentinel in (chan)))
  ([sentinel in out]
     (go (loop [discard true]
           (if-let [v (<! in)]
             (if discard
               (if (<!expand (sentinel v))
                 (recur true)
                 (do
                   (>! out v)
                   (recur false)))
               (do
                 (>! out v)
                 (recur false)))
             (close! out))))
     out))

(defn siphon
  ([in] (siphon in []))
  ([in coll]
    (go (loop [coll coll]
          (if-let [v (<! in)]
            (recur (conj coll v))
            coll)))))

(defn always [v c]
  (let [out (chan)]
    (go (loop []
          (if-let [e (<! c)]
            (do (>! out v)
              (recur))
            (close! out))))
    out))

(defn toggle [in]
  (let [out (chan)
        control (chan)]
    (go (loop [on true]
          (recur
            (alt!
              in ([x] (when on (>! out x)) on)
              control ([x] x)))))
    {:chan out
     :control control}))

(defn barrier [cs]
  (go (loop [cs (seq cs) result []]
        (if cs
          (recur (next cs) (conj result (<! (first cs))))
          result))))

(defn cyclic-barrier [cs]
  (let [out (chan)]
    (go (loop []
          (>! out (<! (barrier cs)))
          (recur)))
    out))

(defn throttle*
  ([in msecs]
    (throttle* in msecs (chan)))
  ([in msecs out]
    (throttle* in msecs out (chan)))
  ([in msecs out control]
    (go
      (loop [state ::init last nil cs [in control]]
        (let [[_ _ sync] cs]
          (let [[v sc] (alts! cs)]
            (condp = sc
              in (condp = state
                   ::init (do (>! out v)
                            (>! out [::throttle v])
                            (recur ::throttling last
                              (conj cs (timeout msecs))))
                   ::throttling (do (>! out v)
                                  (recur state v cs)))
              sync (if last
                     (do (>! out [::throttle last])
                       (recur state nil
                         (conj (pop cs) (timeout msecs))))
                     (recur ::init last (pop cs)))
              control (recur ::init nil
                        (if (= (count cs) 3)
                          (pop cs)
                          cs)))))))
    out))

(defn throttle-msg? [x]
  (and (vector? x)
       (= (first x) ::throttle)))

(defn throttle
  ([in msecs] (throttle in msecs (chan)))
  ([in msecs out]
    (->> (throttle* in msecs out)
      (filter #(and (vector? %) (= (first %) ::throttle)))
      (map second))))

(defn debounce
  ([source msecs]
    (debounce (chan) source msecs))
  ([out source msecs]
    (go
      (loop [state ::init cs [source]]
        (let [[_ threshold] cs]
          (let [[v sc] (alts! cs)]
            (condp = sc
              source (condp = state
                       ::init
                         (do (>! out v)
                           (recur ::debouncing
                             (conj cs (timeout msecs))))
                       ::debouncing
                         (recur state
                           (conj (pop cs) (timeout msecs))))
              threshold (recur ::init (pop cs)))))))
    out))

(defn once [f in]
  (let [out (chan)]
    (go
      (let [val (<! in)]
        (f val)
        (>! out val))
      (loop []
        (if-let [val (<! in)]
          (do (>! out val)
              (recur))
          (close! out))))
    out))

(defn complement [f]
  (fn [& args]
    (go-catch
      (boolean (not (<? (apply f args)))))))

(defn memoize-async [f]
  (let [mem (atom {})]
    (fn [& args]
      (let [v (get @mem args :undefined)]
        (if (= v :undefined)
          (let [c (apply f args)]
            (swap! mem assoc args {:source c})
            (go
              (let [res (<! c)]
                (swap! mem assoc-in [args :value] res)
                res)))
          (go
            (<! (:source v))
            (get-in @mem [args :value])))))))

(defn channel-pool
  ([n] (channel-pool n (chan 2048)))
  ([n queue]
   (dotimes [_ n]
     (dochan [[initializer output] queue]
       (let [input (initializer)]
         (<! (dochan [v input] (>! output v))))
       (close! output)))
   queue))

(defn reduce
  [f init ch]
  (go-catch
    (loop [ret init]
      (let [v (<! ch)]
        (if (nil? v)
          ret
          (recur (<? (f ret v))))))))

(defn pool-enqueue [pool initializer]
  (let [c (chan)]
    (put! pool [initializer c])
    c))

(defn into-list
  ([channels] (into-list channels []))
  ([channels base]
    (async/into base (async/merge channels))))
