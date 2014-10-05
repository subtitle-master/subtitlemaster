(ns sm.core
  (:require-macros [wilkerdev.util.macros :refer [<? go-catch]]
                   [cljs.core.async.macros :refer [go]])
  (:require [wilkerdev.util.nodejs :refer [lstat fopen fread http] :as node]
            [wilkerdev.util :as util]
            [cljs.core.async :refer [<! >! chan close!]]
            [sm.languages :as lang]
            [clojure.string :as str]))

(def Long (js/require "long"))

; low level SubDB API

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

; low level Open Subtitles

(def ^:dynamic *opensub-endpoint* "api.opensubtitles.org")
(def ^:dynamic *opensub-ua* "Subtitle Master v2.0.1.dev")

(defn opensub-hash-section [fd offset]
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

(defn opensub-hash [path]
  (go-catch
    (let [chunk-size (* 64 1024)
          size (.-size (<? (lstat path)))
          end-start (- size chunk-size)
          _ (assert (>= end-start 0) (str "File size must be at least " chunk-size " bytes"))
          file (<? (fopen path "r"))
          sum (-> (<? (opensub-hash-section file 0))
                  (.add (<? (opensub-hash-section file end-start)))
                  (.add (Long. size)))]
      [(.toString sum 16) size])))

(defn opensub-client [] (node/xmlrpc-client {:host *opensub-endpoint* :path "/xml-rpc"}))

(defn opensub-auth [conn]
  (go-catch
    (let [res (<? (node/xmlrpc-call conn "LogIn" "" "" "en" *opensub-ua*))
          status (.-status res)]
      (if (= status "200 OK")
        (.-token res)
        (throw (js/Error. "Can't login on OpenSubtitles"))))))

(defn opensub-search [conn auth query]
  (go-catch
    (let [res (<? (node/xmlrpc-call conn "SearchSubtitles" auth query))
          status (.-status res)]
      (if (= status "200 OK")
        (if-let [data (.-data res)]
          (->> (array-seq data)
               (map util/js->map)))))))

(defn opensub-download-stream [entry]
  (let [url (:sub-download-link entry)]
    (-> (node/http-stream {:uri url})
        (.pipe (.createGunzip node/zlib)))))

; abstractions and integration

(defprotocol SearchProvider
  (search-subtitles [_ path languages]))

(defprotocol Subtitle
  (download-stream [_])
  (subtitle-language [_]))

(defrecord SubDBSubtitle [hash language version]
  Subtitle
  (download-stream [_]
    (subdb-download-stream hash language version))
  (subtitle-language [_] language))

(defn subdb-expand-result [hash {:keys [count language]}]
  (->> (range count)
       (map #(hash-map :language language
                       :hash hash
                       :version %))))

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

(defrecord OpenSubtitlesSubtitle [info]
  Subtitle
  (download-stream [_]
    (opensub-download-stream info))
  (subtitle-language [_]
    (lang/iso-iso639-2b->6391 (:sub-language-id info))))

(defrecord OpenSubtitlesSource [client auth]
  SearchProvider
  (search-subtitles [_ path languages]
    (go-catch
      (let [[hash size] (<? (opensub-hash path))
            query [{:sublanguageid (str/join "," (map lang/iso-6391->iso639-2b languages))
                    :moviehash     hash
                    :moviebytesize size}]
            results (<? (opensub-search client auth query))]
        (map ->OpenSubtitlesSubtitle results)))))

(defn opensub-source []
  (go-catch
    (let [client (opensub-client)
          auth (<? (opensub-auth client))]
      (->OpenSubtitlesSource client auth))))

(defn sources []
  (go-catch
    [(subdb-source) (<? (opensub-source))]))

(defn subtitle-name-pattern [path]
  (let [basename (node/basename-without-extension path)
        quoted (util/quote-regexp basename)]
    (js/RegExp (str "^" quoted "(?:\\.([a-z]{2}))?\\.srt$") "i")))

(defn subtitle-info [path]
  (go-catch
    (let [dirname (node/dirname path)
          files (<? (node/read-dir dirname))
          pattern (subtitle-name-pattern path)
          get-lang (fn [p]
                     (if-let [[_ lang] (re-find pattern p)]
                       (or lang :plain)))]
      {:path path
       :basepath (node/path-join dirname (node/basename-without-extension path))
       :subtitles (set (keep get-lang files))})))

(defn find-first [{:keys [sources path languages]}]
  (go-catch
    (loop [sources sources]
      (if-let [source (first sources)]
        (let [[res] (<? (search-subtitles source path languages))]
          (if res {:subtitle res :source source} (recur (rest sources))))))))

(defn subtitle-target-path [basename lang]
  (if (= lang :plain)
    (str basename ".srt")
    (str basename "." lang ".srt")))

(defn search-download
  ([query] (search-download query (chan)))
  ([{:keys [path sources] :as query} c]
   (go
     (try
       (>! c [:init])
       (let [{:keys [subtitles basepath] :as info} (<? (subtitle-info path))
             query (update-in query [:languages] #(take-while (complement subtitles) %))]
         (>! c [:info info])
         (>! c [:view-path path])
         (if (seq (:languages query))
           (do
             (>! c [:search query])
             (if-let [{:keys [subtitle] :as result} (<? (find-first query))]
               (let [target-path (subtitle-target-path basepath (subtitle-language subtitle))]
                 (>! c [:download (assoc result :target target-path)])
                 (<? (node/save-stream-to (download-stream subtitle) target-path))
                 (>! c [:downloaded]))
               (>! c [:not-found])))
           (>! c [:unchanged])))
       (catch js/Error e (>! c [:error e]))
       (finally (close! c)))
     nil)
   c))
