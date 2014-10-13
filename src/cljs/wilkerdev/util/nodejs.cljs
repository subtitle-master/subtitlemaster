(ns wilkerdev.util.nodejs
  (:require-macros [wilkerdev.util.macros :refer [<? go-catch]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [chan put! close! <!] :as async]
            [wilkerdev.util.reactive :as r]
            [clojure.string :as str]))

(def package-version (.-version (js/require "./package.json")))

(def fs (js/require "fs"))
(def node-request (js/require "request"))
(def node-path (js/require "path"))
(def node-stream (js/require "stream"))
(def node-temp (js/require "temp"))
(def crypto (js/require "crypto"))
(def xmlrpc (js/require "xmlrpc"))
(def zlib (js/require "zlib"))

(def sep (.-sep node-path))

(defn dirname [path] (.dirname node-path path))
(defn extname [path] (.extname node-path path))
(defn basename [path] (.basename node-path path))
(defn basename-without-extension [path] (.basename node-path path (extname path)))

(defn path-join [& paths] (apply (.-join node-path) paths))
(defn basepath [path] (path-join (dirname path) (basename-without-extension path)))

(defn make-js-error [node-err]
  (.log js/console "node err" node-err)
  (if (instance? js/Error node-err)
    node-err
    (js/Error. (.-message node-err))))

(defn node-callback [c]
  (fn [err res]
    (if-not err
      (put! c (or res :done))
      (put! c (make-js-error err)))
    (close! c)))

(defn node->chan* [f & args]
  (let [c (chan 1)
        args (conj (vec args) (node-callback c))
        res (apply f args)]
    [c res]))

(defn node->chan [& args]
  (nth (apply node->chan* args) 0))

(defn node-lift
  ([f] (node-lift f identity))
  ([f transformer]
   (fn [& args]
     (go-catch
       (transformer (<? (apply node->chan f args)))))))

(def rename (node-lift (.-rename fs)))
(def mkdir (node-lift (.-mkdir fs)))
(def lstat (node-lift (.-lstat fs)))
(def fopen (node-lift (.-open fs)))
(def fread (node-lift (.-read fs)))
(def read-file (node-lift (.-readFile fs)))
(def read-dir (node-lift (.-readdir fs) array-seq))

(defn create-read-stream [path] (.createReadStream fs path))
(defn create-write-stream [path] (.createWriteStream fs path))

(def delete-file (node-lift (.-unlink fs)))

(defn http [options]
  (go
    (<! (node->chan node-request (clj->js options)))))

(defn http-stream [options]
  (node-request (clj->js options)))

(defn http-post-form [options builder]
  (let [options (clj->js (merge options {:method        "POST"
                                         :postambleCRLF true}))
        [c req] (node->chan* node-request options)]
    (builder (.form req))
    c))

(defn md5-hex [buffer]
  (let [sum (.createHash crypto "md5")]
    (.update sum buffer)
    (.digest sum "hex")))

(defn md5-file [path]
  (go-catch
    (let [md5 (.createHash crypto "md5")]
      (.update md5 (<? (read-file path)))
      (.digest md5 "hex"))))

(defn xmlrpc-client [options]
  (.createClient xmlrpc (clj->js options)))

(defn xmlrpc-call [client method & args]
  (let [c (chan 1)]
    (.methodCall client method (clj->js args) (node-callback c))
    c))

(defn stream-complete->chan
  ([stream] (stream-complete->chan stream (chan)))
  ([stream c]
   (.on stream "error" #(do (put! c (make-js-error %)) (close! c)))
   (.on stream "end" #(close! c))
   c))

(defn stream->chan
  ([stream] (stream->chan stream (chan 1)))
  ([stream c]
   (stream-complete->chan stream c)
   (.on stream "data" #(put! c %))
   c))

(defn pipe-stream [source target]
  (let [c (stream-complete->chan source)]
    (.pipe source target)
    c))

(defn save-stream-to [stream path]
  (pipe-stream stream (create-write-stream path)))

(defn stream->str [stream] (async/reduce str "" (stream->chan stream)))

(defn make-stream [s]
  (let [Readable (.-Readable node-stream)]
    (doto (Readable.)
          (.push s)
          (.push nil))))

(defn temp-stream [suffix]
  (.createWriteStream node-temp #js {:suffix suffix}))

(defn file-exists? [path]
  (let [out (chan)]
    (.exists fs path #(do (put! out %)
                          (close! out)))
    out))

(defn exists? [path]
  (let [out (chan)]
    (.exists fs path #(do (put! out %)
                          (close! out)))
    out))

(defn is-dir? [path]
  (go-catch
    (let [stat (<? (lstat path))]
      (.isDirectory stat))))

(defn is-file? [path]
  (go-catch
    (let [stat (<? (lstat path))]
      (.isFile stat))))

(defn path-iterator
  ([path] (path-iterator "" (remove str/blank? (str/split path sep))))
  ([current left]
   (if left
     (let [cur (str current sep (first left))]
       (cons cur (lazy-seq (path-iterator cur (next left)))))
     nil)))

(defn mkdir-p [path]
  (let [c (->> (path-iterator path)
               (r/spool)
               (r/drop-while exists?))]
    (go-catch
      (loop []
        (when-let [v (<! c)]
          (<? (mkdir v))
          (recur)))
      :done)))

(defn match-extensions? [path extensions]
  (extensions (-> (extname path)
                  (subs 1))))

(defn read-dir-abs [path]
  (let [fullpath (partial str path sep)]
    (go-catch
      (->> (<? (read-dir path))
           (map fullpath)))))

(defn scan-paths
  ([paths] (scan-paths paths (chan)))
  ([input-paths out]
   (go
     (let [paths (atom (vec input-paths))]
       (while @paths
         (try
           (let [path (peek @paths)]
             (swap! paths next)
             (>! out path)
             (if (<? (is-dir? path)) (swap! paths into (<? (read-dir-abs path)))))
           (catch js/Error e
             (>! out e))))
       (close! out)))
   out))

(defn scandir
  ([path] (scandir path (chan)))
  ([path out] (scan-paths [path] out)))
