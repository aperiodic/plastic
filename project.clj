(defproject cypress "0.2.0"
  :description "Webapp state management with pure event streams, not DOM event handlers."
  :url "https://github.com/aperiodic/cypress"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.3.465"]]

  :source-paths ["src/cljs" "src/cljc"]
  :profiles {:dev {:source-paths ["examples/"]
                   :dependencies [[org.omcljs/om "1.0.0-beta1"]]}}

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.14"]]

  :figwheel {:css-dirs ["resources/public"]}
  :cljsbuild {
    :builds [{
      :id "dev"
      :source-paths ["src/cljs" "src/cljc" "examples/"]
      :figwheel true
      :compiler {
        :main "cypress.examples.custom-events"
        :asset-path "js/out"
        :output-to "resources/public/js/cypress.js"
        :output-dir "resources/public/js/out"
        :optimizations :none
        :pretty-print true}}]})
