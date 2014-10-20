(defproject smgui "2.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}

  :min-lein-version "2.3.4"

  ;; We need to add src/cljs too, because cljsbuild does not add its
  ;; source-paths to the project source-paths
  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.7.0-alpha2"]
                 [org.clojure/clojurescript "0.0-2342"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [om "0.8.0-alpha1"]
                 [camel-snake-kebab "0.2.4"]
                 [com.facebook/react "0.11.2"]
                 [commons-io "2.4"]
                 [clj-http "1.0.0"]
                 [org.clojure/data.json "0.2.5"]
                 [me.raynes/fs "1.4.4"]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-node-webkit-build "0.1.6"]
            [jarohen/simple-brepl "0.1.0"]]

  :eval-in-leiningen true

  :release-tasks [                                          ;["vcs" "assert-committed"]
                  ;["change" "version" "leiningen.release/bump-version" "release"]
                  ;["vcs" "commit"]
                  ;["vcs" "tag"]
                  ;["node-webkit-build"]
                  ["app-release" "github-release"]
                  ;["app-release" "update-package-json"]
                  ;["app-release" "update-website"]
                  ;["app-release" "share"]
                  ;["change" "version" "leiningen.release/bump-version"]
                  ;["vcs" "commit"]
                  ;["vcs" "push"]


                  ]

  :test-paths ["test/cljs"]

  :node-webkit-build {:root                      "public"
                      :name                      "Subtitle Master"
                      :platforms                 #{:osx :win}
                      :osx                       {:icon "resources/subtitle_master.icns"}
                      :nw-version                "0.10.5"
                      :disable-developer-toolbar true
                      :use-lein-project-version  true}

  :cljsbuild  { :builds { :dev { :source-paths ["src/cljs"]
                                 :compiler { :output-to "public/js/smgui.js"
                                             :optimizations :whitespace
                                             :preamble ["react/react.min.js"]
                                             :pretty-print true}}
                          :release { :source-paths ["src/cljs"]
                                     :compiler { :output-to "public/js/smgui.js"
                                                 :output-dir "public/js"
                                                 :optimizations :advanced
                                                 :pretty-print false
                                                 :preamble ["react/react.min.js"]
                                                 :externs ["react/externs/react.js" "externs/nodejs.js"]
                                                 :source-map "public/js/smgui.js.map"}}}})
