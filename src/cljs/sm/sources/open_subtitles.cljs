(ns sm.sources.open-subtitles
  (:require-macros [wilkerdev.util.macros :refer [<? go-catch]])
  (:require [wilkerdev.util.nodejs :refer [lstat fopen fread] :as node]
            [wilkerdev.util :as util]
            [sm.protocols :refer [SearchProvider Subtitle Linkable]]
            [sm.languages :as lang]
            [clojure.string :as str]))

(def Long (js/require "long"))

(def ^:dynamic *opensub-endpoint* "api.opensubtitles.org")
(def ^:dynamic *opensub-ua* (str "Subtitle Master v" node/package-version))

(defn hash-file-section [fd offset]
  (go-catch
    (let [chunk-size (* 64 1024)
          buffer (js/Buffer chunk-size)
          _ (<? (fread fd buffer 0 chunk-size offset))]
      (loop [i 0
             sum (Long. 0 0 true)]
        (when (< i (.-length buffer))
          (let [low (.readUInt32LE buffer i)
                high (.readUInt32LE buffer (+ i 4))]
            (recur (+ i 8)
                   (.add sum (Long. low high)))))
        sum))))

(defn hash-file [path]
  (go-catch
    (let [chunk-size (* 64 1024)
          size (.-size (<? (lstat path)))
          end-start (- size chunk-size)
          _ (assert (>= end-start 0) (str "File size must be at least " chunk-size " bytes"))
          file (<? (fopen path "r"))
          sum (-> (<? (hash-file-section file 0))
                  (.add (<? (hash-file-section file end-start)))
                  (.add (Long. size)))]
      [(.toString sum 16) size])))

(defn client [] (node/xmlrpc-client {:host *opensub-endpoint* :path "/xml-rpc"}))

(defn auth [conn]
  (go-catch
    (let [res (<? (node/xmlrpc-call conn "LogIn" "" "" "en" *opensub-ua*))
          status (.-status res)]
      (if (= status "200 OK")
        (.-token res)
        (throw (js/Error. "Can't login on OpenSubtitles"))))))

(defn search [conn auth query]
  (go-catch
    (let [res (<? (node/xmlrpc-call conn "SearchSubtitles" auth query))
          status (.-status res)]
      (if (= status "200 OK")
        (if-let [data (.-data res)]
          (->> (array-seq data)
               (map util/js->map)))))))

(defn download [entry]
  (let [url (:sub-download-link entry)]
    (-> (node/http-stream {:uri url})
        (.pipe (.createGunzip node/zlib)))))

(defrecord OpenSubtitlesSubtitle [info]
  Subtitle
  (download-stream [_]
    (download info))
  (subtitle-language [_]
    (lang/iso-iso639-2b->6391 (:sub-language-id info))))

(defrecord OpenSubtitlesSource [client auth-atom]
  INamed
  (-name [_] "Open Subtitles")

  Linkable
  (-linkable-url [_] "http://www.opensubtitles.com")

  SearchProvider
  (search-subtitles [_ path languages]
    (go-catch
      (if-not @auth-atom
        (reset! auth-atom (<? (auth client))))
      (let [[hash size] (<? (hash-file path))
            query [{:sublanguageid (str/join "," (map lang/iso-6391->iso639-2b languages))
                    :moviehash     hash
                    :moviebytesize size}]]
        (->> (<? (search client @auth-atom query))
             (map ->OpenSubtitlesSubtitle))))))

(defn source []
  (->OpenSubtitlesSource (client) (atom nil)))
