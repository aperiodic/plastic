(ns cypress.examples.hello-world
  (:require [cypress.core :as cyp]
            [cypress.state-machine :as sm]))

(enable-console-print!)

(defn sandbox-node
  []
  (.getElementById js/document "sandbox"))

(defn clear-sandbox!
  []
  (let [sandbox (sandbox-node)
        new-sandbox (.cloneNode sandbox)]
    (.replaceChild (.-parentNode sandbox) new-sandbox sandbox)))

(def click-unclick
  (-> (sm/blank-state-machine :idle)
    (sm/add-transition :idle :active :mouse-down)
    (sm/add-transition :active :idle :mouse-up)))

(defn start!
  []
  (cyp/init (sandbox-node) click-unclick))

(clear-sandbox!)
(start!)
