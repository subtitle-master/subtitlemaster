(defproject smgui "0.0.1-SNAPSHOT"
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
                 [om "0.7.3"]
                 [camel-snake-kebab "0.2.4"]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-node-webkit-build "0.1.2-SNAPSHOT"]
            [jarohen/simple-brepl "0.1.0"]]

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
                                             :pretty-print true}}}})
