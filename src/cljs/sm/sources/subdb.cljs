(ns sm.sources.subdb
  (:require-macros [wilkerdev.util.macros :refer [<? go-catch]])
  (:require [sm.protocols :refer [SearchProvider UploadProvider Subtitle Linkable]]
            [wilkerdev.util.nodejs :refer [lstat fopen fread http] :as node]
            [wilkerdev.util :as util]))

(def ^:dynamic *subdb-endpoint* "http://api.thesubdb.com/")
(def ^:dynamic *subdb-ua* (str "SubDB/1.0 (Subtitle Master/" node/package-version "; http://subtitlemaster.com)"))

(defn hash-file [path]
  (go-catch
    (let [chunk-size (* 64 1024)
          size (.-size (<? (lstat path)))
          end-start (- size chunk-size)
          _ (assert (>= end-start 0) (str "File size must be at least " chunk-size " bytes"))
          file (<? (fopen path "r"))
          buffer (js/Buffer. (* 2 chunk-size))]

      (<? (fread file buffer 0 chunk-size 0))
      (<? (fread file buffer chunk-size chunk-size end-start))

      (node/md5-hex buffer))))

(defn query
  ([action] (query action {}))
  ([action options]
   (let [query (util/map->query (merge {:action action} options))
         uri (str *subdb-endpoint* "?" query)]
     {:uri     uri
      :headers {"User-Agent" *subdb-ua*}})))

(defn search-languages [hash]
  (go-catch
    (let [params (query "search" {:hash hash :versions ""})
          response (<? (http params))
          status (.-statusCode response)
          parse-language (fn [l] (let [[lang count] (.split l ":")]
                                   {:language lang
                                    :count    (js/parseInt count)}))]
      (if (= 200 status)
        (map parse-language (-> response
                                .-body
                                (.split ",")))
        []))))

(defn download
  ([hash language] (download hash language 0))
  ([hash language version]
   (let [params (query "download" {:hash hash :version version :language (.replace language "pb" "pt")})]
     (node/http-stream params))))

(defn upload [hash stream]
  (go-catch
    (let [params (query "upload")
          build-form (fn [f]
                       (.append f "hash" hash)
                       (.append f "file" stream #js {:contentType "application/octet-stream"}))
          response (<? (node/http-post-form params build-form))]
      (case (.-statusCode response)
        201 :uploaded
        403 :duplicated
        415 :invalid
        400 :malformed
        :unknown))))

(defn expand-result [hash {:keys [count language]}]
  (->> (range count)
       (map #(hash-map :language language
                       :hash hash
                       :version %))))

(defn normalize-language [languages lang]
  (if (and (= lang "pt") (languages "pb"))
    "pb"
    lang))

(defrecord SubDBSubtitle [hash language version]
  Subtitle
  (download-stream [_]
    (download hash language version))
  (subtitle-language [_] language))

(defn process-search-result [languages query-languages hash]
  (comp (filter #(languages (:language %)))
        (mapcat (partial expand-result hash))
        (map #(update-in % [:language] (partial normalize-language query-languages)))
        (map map->SubDBSubtitle)))

(defrecord SubDBSource []
  INamed
  (-name [_] "SubDB")

  Linkable
  (-linkable-url [_] "http://www.subdb.net")

  SearchProvider
  (search-subtitles [_ path languages]
    (go-catch
      (let [query-languages (mapv (fn [x] (.replace x "pb" "pt")) languages)
            hash (<? (hash-file path))
            results (<? (search-languages hash))]
        (sequence (process-search-result (set query-languages)
                                         (set languages)
                                         hash) results))))

  UploadProvider
  (upload-subtitle [_ path sub-path]
    (go-catch
      (let [hash (<? (hash-file path))
            read-stream (node/create-read-stream sub-path)]
        (<? (upload hash read-stream))))))

(defn source [] (->SubDBSource))
