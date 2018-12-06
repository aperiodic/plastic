(defproject cypress/drag-and-drop "0.1.0-SNAPSHOT"
  :description "A simple example of the kinds of tangibly interactive webapps cypress was designed to enable."
  :url "https://example.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.3.465"]
                 [org.omcljs/om "1.0.0-beta1"]
                 [cypress "0.3.1-SNAPSHOT"]]

  :source-paths ["src/cljs" "src/cljc"]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.14"]]

  :figwheel {:css-dirs ["resources/public"]}
  :cljsbuild {
    :builds [{
      :id "dev"
      :source-paths ["src/cljs"]
      :figwheel true
      :compiler {
        :main "cypress.examples.drag-and-drop"
        :asset-path "js/out"
        :output-to "resources/public/js/drag-and-drop.js"
        :output-dir "resources/public/js/out"
        :optimizations :none
        :pretty-print true}}]})
