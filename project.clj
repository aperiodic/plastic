(defproject plastic "0.3.1-SNAPSHOT"
  :description "Webapp state definition with pure event streams, not DOM event handlers."
  :url "https://github.com/aperiodic/plastic"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.3.465"]]

  :source-paths ["src/cljs" "src/cljc"]
  :profiles {:dev {:source-paths ["dev-sandbox"]
                   :dependencies [[org.omcljs/om "1.0.0-beta1"]]}}

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.14"]]

  :figwheel {:css-dirs ["resources/public"]}
  :cljsbuild {
    :builds [{
      :id "dev"
      :source-paths ["src/cljs" "src/cljc" "dev-sandbox"]
      :figwheel true
      :compiler {
        :main "plastic.sandbox.main"
        :asset-path "js/out"
        :output-to "resources/public/js/plastic.js"
        :output-dir "resources/public/js/out"
        :optimizations :none
        :pretty-print true}}]})
