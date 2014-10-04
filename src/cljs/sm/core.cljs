(ns sm.core
  (:require-macros [wilkerdev.util.macros :refer [<? go-catch]])
  (:require [wilkerdev.util.nodejs :refer [lstat fopen fread http] :as node]))

(def ^:dynamic *subdb-endpoint* "http://api.thesubdb.com/")

(defn map->query [m]
  (.toString (.createFromMap goog.Uri.QueryData (clj->js m))))

(def subdb-ua "SubDB/1.0 (Subtitle Master/2.0.1; http://subtitlemaster.com)")

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
   (let [query (map->query (merge {:action action} options))
         uri (str *subdb-endpoint* "?" query)]
     {:uri     uri
      :headers {"User-Agent" subdb-ua}})))

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
                                (.split ",")))))))

(defn subdb-download
  ([hash language] (subdb-download hash language 0))
  ([hash language version]
   (go-catch
     (let [params (subdb-query "download" {:hash hash :version version :language language})
           response (<? (http params))
           status (.-statusCode response)]
       (if (= 200 status)
         (.-body response))))))

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
