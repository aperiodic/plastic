(ns cypress.examples.hello-world
  (:require [cypress.core :as cyp]))

(enable-console-print!)

(defn sandbox-node
  []
  (.getElementById js/document "sandbox"))

(defn clear-sandbox!
  []
  (let [sandbox (sandbox-node)
        new-sandbox (.cloneNode sandbox)]
    (.replaceChild (.-parentNode sandbox) new-sandbox sandbox)))

(defn start!
  []
  (cyp/init (sandbox-node)))

(clear-sandbox!)
(start!)
