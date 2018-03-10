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

(defn add-down
  [app-state ui-state triggering-event]
  (update app-state :downs inc))

(defn add-up
  [app-state ui-state triggering-event]
  (update app-state :ups inc))

(def click-unclick
  (-> (sm/blank-state-machine :idle)
    (sm/add-transition :idle :active :mouse-down add-down)
    (sm/add-transition :active :idle :mouse-up add-up)))

(defn start!
  []
  (cyp/init (sandbox-node) click-unclick {:ups 0, :downs 0}))

(clear-sandbox!)
(start!)
