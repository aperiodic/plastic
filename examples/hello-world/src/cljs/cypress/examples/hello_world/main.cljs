(ns cypress.examples.hello-world.main
  (:require [cypress.examples.hello-world.core :as core]
            [cypress.core :as cyp]
            [cypress.state-machine :as sm]
            [om.core :as om]
            [om.dom :as dom]))

(defonce !state (atom {:ups 0, :downs 0}))

(defn app-node
  []
  (.getElementById js/document "hello-world"))

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
  (cyp/init! (app-node) [] click-unclick !state {:logging true})
  (om/root
    (fn [state _owner]
      (reify
        om/IRender
        (render [_]
          (dom/p nil
                 (str "Click me!"
                      " ↓:" (:downs state)
                      " ↑:" (:ups state) )))))
    !state
    {:target (app-node)}))

(defn clear-app-node!
  "Remove all the event handlers registered on the app's base DOM node by
  replacing it with a clone. This is only necessary when using figwheel because
  it reloads the namespace without refreshing the DOM."
  []
  (let [old-node (app-node)]
    (.replaceChild (.-parentNode old-node) (.cloneNode old-node true) old-node)))

(enable-console-print!)
(clear-app-node!)
(start!)
