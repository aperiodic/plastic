(ns cypress.examples.dispatched
  (:require [cypress.core :as cyp]
            [cypress.state-machine :as sm]
            [om.core :as om]
            [om.dom :as dom]))

(defonce !state (atom {:1-ups 0, :2-ups 0, :downs 0}))

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

(defn up-adder
  [from]
  (fn [app-state ui-state triggering-event]
    (update app-state (if (= from :active-1) :1-ups :2-ups) inc)))

(def click-unclick
  (-> (sm/blank-state-machine :idle)
    (sm/add-transition :idle #(rand-nth [:active-1 :active-2]) :mouse-down add-down)
    (sm/add-transition :active-1 :idle :mouse-up (up-adder :active-1))
    (sm/add-transition :active-2 :idle :mouse-up (up-adder :active-2))))


(defn start!
  []
  (cyp/init! (sandbox-node) click-unclick !state)
  (om/root
    (fn [state _owner]
      (reify
        om/IRender
        (render [_]
          (dom/p nil
                 "↓:" (:downs state)
                 (dom/br nil)
                 " 1↑:" (:1-ups state)
                 (dom/br nil)
                 " 2↑:" (:2-ups state)))))
    !state
    {:target (.getElementById js/document "sandbox")}))

(enable-console-print!)
(clear-sandbox!)
(start!)
