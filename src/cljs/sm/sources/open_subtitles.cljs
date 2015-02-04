(ns sm.sources.open-subtitles
  (:require-macros [wilkerdev.util.macros :refer [<? go-catch]])
  (:require [wilkerdev.util.nodejs :refer [lstat fopen fread] :as node]
            [wilkerdev.util :as util]
            [wilkerdev.util.reactive :as r]
            [sm.util :as sm-util]
            [sm.protocols :refer [SearchProvider UploadProvider Subtitle Linkable]]
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

(defn build-query [path languages]
  (go-catch
    (let [lang (str/join "," (map lang/iso-6391->iso639-2b languages))
          [hash size] (<? (hash-file path))
          hash-query {:sublanguageid lang
                      :moviehash     hash
                      :moviebytesize size}
          tag-query {:sublanguageid lang
                     :tag           (node/basename path)}
          text-query (if-let [{:keys [name season episode]} (sm-util/episode-info path)]
                       {:sublanguageid lang
                        :query         name
                        :season        (js/parseInt season)
                        :episode       (js/parseInt episode)})]
      (filter identity [hash-query text-query tag-query]))))

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

; TODO: implement auto-search when it's working
(defn auto-search [conn auth query])

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

(defn- path->base64 [path]
  (go-catch
    (let [reader (node/create-read-stream path)]
      (-> (node/stream->buffer reader) <?
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
                              (assoc-in [:cd1 :subcontent] (<? (path->base64 sub-path))))
              response (<? (call conn "UploadSubtitles" auth upload-info))]
          (if (= "200 OK" (.-status response))
            :uploaded
            :unknown))
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
    (.log js/console "Searching to OpenSubtitles" path (clj->js languages))
    (go-catch
      (let [auth-token (<? (m-auth client))
            query (<? (build-query path languages))]
        (->> (<? (search client auth-token query))
             (map ->OpenSubtitlesSubtitle)))))

  UploadProvider
  (upload-subtitle [_ path sub-path]
    (.log js/console "Uploading to OpenSubtitles" path sub-path)
    (go-catch
      (let [res (<? (upload {:conn     client
                             :auth     (<? (m-auth client))
                             :path     path
                             :sub-path sub-path}))]
        (.log js/console "Upload response" (clj->js res))
        res))))

(def AUTH-TIMEOUT (* 3 60 1000))

(defn source []
  (->OpenSubtitlesSource (client) (r/memoize-timeout auth AUTH-TIMEOUT)))
