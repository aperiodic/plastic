(defproject plastic/click-game "0.1.0-SNAPSHOT"
  :description "A simple click-the-dots game to show how to add logic to state machine transitions."
  :url "https://example.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.3.465"]
                 [org.omcljs/om "1.0.0-beta1"]
                 [plastic "0.3.0"]]

  :source-paths ["src/cljs"]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.14"]]

  :figwheel {:css-dirs ["resources/public"]}
  :cljsbuild {
    :builds [{
      :id "dev"
      :source-paths ["src/cljs"]
      :figwheel true
      :compiler {
        :main "plastic.examples.click-game"
        :asset-path "js/out"
        :output-dir "resources/public/js/out"
        :output-to "resources/public/js/game.js"
        :optimizations :none
        :pretty-print true}}]})
