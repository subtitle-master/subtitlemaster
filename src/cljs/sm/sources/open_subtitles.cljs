(ns sm.sources.open-subtitles
  (:require-macros [wilkerdev.util.macros :refer [<? go-catch]])
  (:require [wilkerdev.util.nodejs :refer [lstat fopen fread] :as node]
            [wilkerdev.util :as util]
            [wilkerdev.util.reactive :as r]
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

(defn call [& args]
  (r/retry #(apply node/xmlrpc-call args) 5))

(defn auth [conn]
  (go-catch
    (let [res (<? (call conn "LogIn" "" "" "en" *opensub-ua*))
          status (.-status res)]
      (if (= status "200 OK")
        (.-token res)
        (throw (js/Error. "Can't login on OpenSubtitles"))))))

(defn search [conn auth query]
  (go-catch
    (let [res (<? (call conn "SearchSubtitles" auth query))
          status (.-status res)]
      (if (= status "200 OK")
        (if-let [data (.-data res)]
          (->> (array-seq data)
               (map util/js->map)))))))

(defn download [entry]
  (let [url (:sub-download-link entry)]
    (-> (node/http-stream {:uri url})
        (.pipe (.createGunzip node/zlib)))))

(defn upload-data [{:keys [path sub-path]}]
  (go-catch
    (let [[moviehash moviebytesize] (<? (hash-file path))]
      {:cd1 {:subhash       (<? (node/md5-file sub-path))
             :subfilename   (node/basename sub-path)
             :moviehash     moviehash
             :moviebytesize moviebytesize
             :moviefilename (node/basename path)}})))

(defn- path->gzip64 [path]
  (go-catch
    (let [reader (node/create-read-stream path)
          gzip (node/create-deflate-raw)]
      (.pipe reader gzip)
      (-> (node/stream->buffer gzip) <?
          (.toString "base64")))))

(defn upload [{:keys [conn auth sub-path] :as query}]
  (go-catch
    (let [upload-info (<? (upload-data query))
          try-result (<? (call conn "TryUploadSubtitles" auth upload-info))
          pristine? (= (.-alreadyindb try-result) 0)
          data (util/js->map (aget (.-data try-result) 0))]
      (if pristine?
        (let [upload-info (-> upload-info
                              (assoc :baseinfo {:idmovieimdb (:id-movie-imdb data)})
                              (assoc-in [:cd1 :subcontent] (<? (path->gzip64 sub-path))))
              response (<? (call conn "UploadSubtitles" auth upload-info))]
          (.log js/console (clj->js upload-info))
          response)
        :duplicated))))

(defrecord OpenSubtitlesSubtitle [info]
  Subtitle
  (download-stream [_]
    (download info))
  (subtitle-language [_]
    (lang/iso-iso639-2b->6391 (:sub-language-id info))))

(defrecord OpenSubtitlesSource [client m-auth]
  INamed
  (-name [_] "Open Subtitles")

  Linkable
  (-linkable-url [_] "http://www.opensubtitles.com")

  SearchProvider
  (search-subtitles [_ path languages]
    (go-catch
      (let [auth-token (<? (m-auth client))
            [hash size] (<? (hash-file path))
            query [{:sublanguageid (str/join "," (map lang/iso-6391->iso639-2b languages))
                    :moviehash     hash
                    :moviebytesize size}]]
        (->> (<? (search client auth-token query))
             (map ->OpenSubtitlesSubtitle))))))

(defn source []
  (->OpenSubtitlesSource (client) (r/memoize-async auth)))
