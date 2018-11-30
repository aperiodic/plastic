(ns cypress.examples.calculator
  (:require [cljs.core.async :as async]
            [cypress.core :as cyp]
            [cypress.state-machine :as sm]
            [cypress.examples.calculator.core :as calc :refer [calculator new-state]]
            [om.core :as om]
            [om.dom :as dom]))

;;
;; DOM Hooks
;;

(defn node-with-id
  [id]
  (.getElementById js/document (str id)))

(defn root-node
  []
  (node-with-id "display"))

(defn digit-node
  [digit]
  (node-with-id (str "btn-" digit)))

(defn op-node
  [op]
  (node-with-id (name op)))

(defn add-emitter!
  [element event-name emitted-event channel]
  (.addEventListener element event-name
    (fn [_]
      (async/put! channel emitted-event))
    #js {:passive false})
  nil)

;;
;; DOM Rendering
;;

(defn format-value
  [n]
  (str n))

(defn display-value
  [state]
  (case (:ui-state state)
    :zero 0
    :result (:result state)
    :choose-op (or (:entry state) (:result state) 0)
    (:1st-arg :nth-arg :identity) (:entry state)))

(defn operation-symbol
  [state]
  (case (:ui-state state)
    (:zero :identity :1st-arg) nil
    :result (calc/op-name (get-in state [:mem :operation :kw]))
    (:choose-op :nth-arg) (let [op-kw (get-in state [:operation :kw])]
                            (calc/op-name op-kw))))

(defn render-display
  [state]
  (dom/div #js {:id "display-wrapper"}
    (dom/div #js {:id "operation"}
      (operation-symbol state))
    (dom/div #js {:id "value"}
      (-> state display-value format-value))))

;;
;; Main
;;

(defonce !state (atom (new-state)))

(defn add-calculator-event-emitters!
  []
  (let [digits-channel (async/chan 2)
        operations-channel (async/chan 2)
        c-channel (async/chan 2)
        ac-channel (async/chan 2)
        equals-channel (async/chan 2)]
    ;; hook up the digit buttons
    (doseq [i (range 0 10)]
      (let [event {:value i}]
        (add-emitter! (digit-node i) "mousedown" event digits-channel)))
    ;; hook up the operation buttons
    (doseq [op [:add :subtract :multiply :divide]]
      (let [event {:operation op}]
        (add-emitter! (op-node op) "mousedown" event operations-channel)))
    ;; hook up the 'C', 'AC', and '=' buttons
    (add-emitter!
      (node-with-id "clear") "mousedown" :clear-entry c-channel)
    (add-emitter!
      (node-with-id "all-clear") "mousedown" :clear-all ac-channel)
    (add-emitter!
      (node-with-id "equals") "mousedown" :equals equals-channel)
    ;; return mapping of event kind to event channel
    {:digit digits-channel
     :operation operations-channel
     :clear-entry c-channel
     :clear-all ac-channel
     :equals equals-channel}))

(defn start!
  []
  (let [custom-event-channels (add-calculator-event-emitters!)]
    (cyp/init! (root-node) custom-event-channels calculator !state
               {:logging true}))
  (om/root
    (fn [state _owner]
      (reify
        om/IRender
        (render [_]
          (render-display state))))
    !state
    {:target (root-node)}))

(defn clear-node!
  "Remove all the event handlers registered on the app's base DOM node by
  replacing it with a clone. This is only necessary when using figwheel because
  it reloads the namespace without refreshing the DOM."
  [node]
  (.replaceChild (.-parentNode node) (.cloneNode node true) node))

(defn -main
  [& _]
  (enable-console-print!)
  (clear-node! (node-with-id "calculator"))
  (start!))

(-main)
