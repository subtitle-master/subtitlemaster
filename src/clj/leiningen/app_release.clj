(ns leiningen.app-release
  (:require [leiningen.core.main :as main]
            [leiningen.cljsbuild :refer [cljsbuild]]
            [releaser.core :as releaser]))

(defn github-release [project]
  (releaser/github-release project))

(defn update-package-json [project]
  (println "project version" (:version project)))

(defn- not-found [subtask]
  (partial #'main/task-not-found (str "app-release " subtask)))

(defn ^{:subtasks [#'update-package-json #'github-release]} app-release [project subtask & args]
  (let [subtasks (:subtasks (meta #'app-release) {})
        [subtask-var] (filter #(= subtask (name (:name (meta %)))) subtasks)]
    (apply (or subtask-var (not-found subtask)) project args)))
