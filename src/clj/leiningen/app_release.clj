(ns leiningen.app-release
  (:import (java.io File))
  (:require [leiningen.core.main :as main]
            [leiningen.cljsbuild :refer [cljsbuild]]
            [leiningen.node-webkit-build :refer [node-webkit-build]]
            [releaser.core :as releaser]))

(defn github-release [project]
  (let [build-info (node-webkit-build project)
        auth [(System/getenv "GH_UN") (System/getenv "GH_PW")]
        uploads (releaser/github-release auth build-info)
        info-path "tmp/github-release-info.edn"]
    (spit (File. info-path) (pr-str uploads))
    (println "Release done, info saved at:" info-path)))

(defn update-package-json [project]
  (println "project version" (:version project)))

(defn release-app [project]
  (let [auth [(System/getenv "GH_UN") (System/getenv "GH_PW")]
        build-info (node-webkit-build project)
        release-info (releaser/github-release auth build-info)
        update-info (releaser/generate-latest-info build-info release-info)]
    ))

(defn- not-found [subtask]
  (partial #'main/task-not-found (str "app-release " subtask)))

(defn ^{:subtasks [#'update-package-json #'github-release]} app-release [project subtask & args]
  (let [subtasks (:subtasks (meta #'app-release) {})
        [subtask-var] (filter #(= subtask (name (:name (meta %)))) subtasks)]
    (apply (or subtask-var (not-found subtask)) project args)))
