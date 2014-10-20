(ns releaser.core
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [clojure.core.async :as async])
  (:import (org.apache.commons.io.input CountingInputStream)))

(defn gh-post [{:keys [auth url params]}]
  (let [res (http/post (str "https://api.github.com/" url)
                       {:form-params  params
                        :basic-auth   auth
                        :content-type :json
                        :headers      {"Accept" "application/vnd.github.v3+json"}})]
    (update-in res [:body] #(json/read-str % :key-fn keyword))))

(defn gh-upload [{:keys [url name path auth]}]
  (let [c (async/chan 1)
        url (clojure.string/replace url "{?name}" "")
        stream (CountingInputStream. (io/input-stream path))
        size (fs/size path)]
    (async/thread
      (let [result (http/post url
                              {:basic-auth   auth
                               :query-params {"name" name}
                               :body         stream
                               :headers      {"Accept"         "application/vnd.github.v3+json"
                                              "Content-Type"   "application/zip"}
                               :length       size
                               :debug        true})]
        (async/>!! c result)
        (async/close! c)))
    (async/go
      (loop []
        (let [sent (.getByteCount stream)]
          (when (< sent size)
            (async/>! c {:total size :sent sent :progress true})
            (async/<! (async/timeout 1000))
            (recur)))))
    c))

(defn gh-create-release [auth options]
  (gh-post {:auth   auth
            :url    "repos/subtitle-master/subtitlemaster/releases"
            :params options}))

(defn github-release [project]
  (let [auth nil
        upload-url (-> (gh-create-release auth
                                          {:tag_name "2.0.1"
                                           :name     "Subtitle Master v2.0.1"
                                           :draft    true})
                       (get-in [:body :upload_url]))
        releases (->> (slurp "tmp/nw-build/build-info.edn")
                      read-string
                      :builds
                      (map (fn [[k v]] [k (:compressed-path v)])))]
    (doseq [[os path] (take 1 releases)]
      (let [ch (gh-upload {:url  upload-url
                           :name (fs/base-name path)
                           :path path
                           :auth auth})]
        (println "Uploading" os path)
        #_ (loop []
          (when-let [{:keys [total sent progress] :as res} (async/<!! ch)]
            (if progress
              (do
                (println "Total " total)
                (println "Loaded" sent)
                (recur))
              (do
                (println "Done")
                (clojure.pprint/pprint res)))))))))
