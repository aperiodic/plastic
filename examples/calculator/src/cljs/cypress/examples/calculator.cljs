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
  (node-with-id (name op))
  #_(case op
    + (node-with-id "add")
    - (node-with-id "subtract")
    * (node-with-id "multiply")
    / (node-with-id "divide")))

(defn emit-on-chan
  [element event-name emitted-event]
  (let [out (async/chan 4)]
    (.addEventListener element event-name
      (fn [_]
        (async/put! out emitted-event))
      #js {:passive false})
    out))

;;
;; DOM Rendering
;;

;; ooh, the only thing we want to render is the display; everything else
;; is static

(defn format-value
  [n]
  (str n))

(defn display-value
  [state]
  (case (:ui-state state)
    :zero 0
    :result (:result state)
    :choose-operation (or (:entry state) (:result state))
    (:1st-arg :nth-arg :identity) (:entry state)))

(defn operation-symbol
  [state]
  (case (:ui-state state)
    (:zero :identity :1st-arg) nil
    :result (calc/op-name (get-in state [:mem :operation :kw]))
    (:choose-operation :nth-arg) (let [op-kw (get-in state [:operation :kw])]
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

(defn add-event-emitters!
  []
  (let [digit-channels (-> (for [i (range 0 10)]
                             (let [btn (digit-node i)
                                   event {:kind :digit, :value i}]
                               (emit-on-chan btn "mousedown" event)))
                         doall)
        op-channels (for [op [:add :subtract :multiply :divide]]
                      (let [btn (op-node op)
                            event {:kind :operation, :operation op}]
                        (emit-on-chan btn "mousedown" event)))]
    (concat
      digit-channels
      op-channels
      [(emit-on-chan (node-with-id "clear") "mousedown" {:kind :clear-entry})
       (emit-on-chan (node-with-id "all-clear") "mousedown" {:kind :clear-all})
       (emit-on-chan (node-with-id "equals") "mousedown" {:kind :equals})])))

(defn start!
  []
  (cyp/init! (root-node) (add-event-emitters!) calculator !state
             {:logging true})
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
