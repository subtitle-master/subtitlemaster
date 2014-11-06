(ns releaser.core
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [clojure.core.async :as async]
            [clj-progress.core :as progress]
            [cheshire.core :refer [generate-string]])
  (:import (org.apache.commons.io.input CountingInputStream)
           (java.io File)))

(defn upload-with-progress* [url path options]
  (let [c (async/chan 1)
        stream (CountingInputStream. (io/input-stream path))
        size (fs/size path)]
    (progress/init (str "Uploading " (fs/base-name path)) size)
    (async/thread
      (let [options (merge {:body stream :length size} options)
            result (http/post url options)]
        (async/>!! c result)
        (async/close! c)))
    (async/go
      (loop []
        (let [sent (.getByteCount stream)]
          (if (< sent size)
            (do
              (if (> sent 0) (progress/tick-to sent))
              (async/>! c {:total size :sent sent :progress true})
              (async/<! (async/timeout 1000))
              (recur))
            (do
              (progress/tick-to size)
              (progress/done))))))
    c))

(defn upload-with-progress [url path options]
  (let [ch (upload-with-progress* url path options)]
    (loop []
      (when-let [{:keys [progress] :as res} (async/<!! ch)]
        (if progress
          (recur)
          res)))))

(defn handle-gh-response [res] (update-in res [:body] #(json/read-str % :key-fn keyword)))

(defn gh-post [{:keys [auth url params]}]
  (let [res (http/post (str "https://api.github.com/" url)
                       {:form-params  params
                        :basic-auth   auth
                        :content-type :json
                        :insecure?    true
                        :headers      {"Accept" "application/vnd.github.v3+json"}})]
    (handle-gh-response res)))

(defn gh-patch [{:keys [auth url params]}]
  (let [res (http/patch (str "https://api.github.com/" url)
                        {:form-params  params
                         :basic-auth   auth
                         :content-type :json
                         :insecure?    true
                         :headers      {"Accept" "application/vnd.github.v3+json"}})]
    (handle-gh-response res)))

(defn gh-create-release [auth options]
  (println "Creating release" (:name options))
  (gh-post {:auth   auth
            :url    "repos/subtitle-master/subtitlemaster/releases"
            :params options}))

(defn gh-publish-release [auth {{:keys [name id]} :body}]
  (println "Publishing release" name)
  (gh-patch {:auth   auth
             :url    (str "repos/subtitle-master/subtitlemaster/releases/" id)
             :params {:draft false}}))

(defn gh-upload-release-with-progress [{:keys [auth release path]}]
  (let [upload-url (-> (get-in release [:body :upload_url])
                       (clojure.string/replace "{?name}" ""))
        name (fs/base-name path)
        options {:basic-auth   auth
                 :query-params {"name" name}
                 :insecure?    true
                 :headers      {"Accept"       "application/vnd.github.v3+json"
                                "Content-Type" "application/zip"}}]
    (handle-gh-response (upload-with-progress upload-url path options))))

(defn github-release [auth lein-build-info]
  (let [version (get-in lein-build-info [:package :version])
        name (get-in lein-build-info [:package :name])
        releases (map (fn [[k v]] [k (:compressed-path v)])
                      (:builds lein-build-info))
        release (gh-create-release auth
                                   {:tag_name (str "v" version)
                                    :name     (str name " v" version)
                                    :draft    true})
        uploads (for [[os path] releases
                      :let [upload (gh-upload-release-with-progress {:auth    auth
                                                                     :release release
                                                                     :path    path})]]
                  [os upload])
        upload-map (into {} uploads)]
    #_ (gh-publish-release auth release)
    {:release release
     :uploads (into {} upload-map)}))

(defn generate-latest-info [build-info release-info]
  (let [data {:name        (get-in build-info [:package :name])
              :version     (str "v" (get-in build-info [:package :version]))
              :manifestUrl "https://raw.githubusercontent.com/subtitle-master/subtitlemaster/master/latest.json"
              :packages    {:mac (get-in release-info [:uploads :osx :body :browser_download_url])
                            :win (get-in release-info [:uploads :win :body :browser_download_url])}}]
    data))
