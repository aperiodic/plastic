(defproject cypress/hello-world "0.1.0-SNAPSHOT"
  :description "One of the simplest possible apps using cypress."
  :url "https://example.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.3.465"]
                 [org.omcljs/om "1.0.0-beta1"]
                 [cypress "0.2.0"]]

  :source-paths ["src/cljs" "src/cljc"]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.14"]]

  :figwheel {:css-dirs ["resources/public"]}
  :cljsbuild {
    :builds [{
      :id "dev"
      :source-paths ["src/cljs" "src/cljc"]
      :figwheel true
      :compiler {
        :main "cypress.examples.hello-world.main"
        :asset-path "js/out"
        :output-to "resources/public/js/hello_world.js"
        :output-dir "resources/public/js/out"
        :optimizations :none
        :pretty-print true}}]})
