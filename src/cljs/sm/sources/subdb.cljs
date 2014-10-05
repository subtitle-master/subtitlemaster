(ns sm.sources.subdb
  (:require-macros [wilkerdev.util.macros :refer [<? go-catch]])
  (:require [sm.protocols :refer [SearchProvider Subtitle]]
            [wilkerdev.util.nodejs :refer [lstat fopen fread http] :as node]
            [wilkerdev.util :as util]))

(def ^:dynamic *subdb-endpoint* "http://api.thesubdb.com/")
(def ^:dynamic *subdb-ua* "SubDB/1.0 (Subtitle Master/2.0.1; http://subtitlemaster.com)")

(defn subdb-hash [path]
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

(defn subdb-query
  ([action] (subdb-query action {}))
  ([action options]
   (let [query (util/map->query (merge {:action action} options))
         uri (str *subdb-endpoint* "?" query)]
     {:uri     uri
      :headers {"User-Agent" *subdb-ua*}})))

(defn subdb-search-languages [hash]
  (go-catch
    (let [params (subdb-query "search" {:hash hash :versions ""})
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

(defn subdb-download-stream
  ([hash language] (subdb-download-stream hash language 0))
  ([hash language version]
   (let [params (subdb-query "download" {:hash hash :version version :language language})]
     (node/http-stream params))))

(defn subdb-upload [hash stream]
  (go-catch
    (let [params (subdb-query "upload")
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

(defn subdb-expand-result [hash {:keys [count language]}]
  (->> (range count)
       (map #(hash-map :language language
                       :hash hash
                       :version %))))

(defrecord SubDBSubtitle [hash language version]
  Subtitle
  (download-stream [_]
    (subdb-download-stream hash language version))
  (subtitle-language [_] language))

(defrecord SubDBSource []
  SearchProvider
  (search-subtitles [_ path languages]
    (go-catch
      (let [languages (mapv (fn [x] (.replace x "pb" "pt")) languages)
            lang-set (set languages)
            hash (<? (subdb-hash path))
            results (<? (subdb-search-languages hash))]
        (->> (filter #(lang-set (:language %)) results)
             (mapcat (partial subdb-expand-result hash))
             (map map->SubDBSubtitle))))))

(defn subdb-source [] (->SubDBSource))
