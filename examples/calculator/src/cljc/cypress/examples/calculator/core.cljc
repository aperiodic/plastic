(ns cypress.examples.calculator.core
  (:require [cypress.state-machine :as sm]
            [cypress.examples.calculator.util :refer [invert-map]]))


(def key->op
  {:add      +
   :subtract -
   :multiply *
   :divide   /})

(def op->key (invert-map key->op))

(defn op-name
  [op-key]
  (case op-key
    :add      "+"
    :subtract "-"
    :multiply "*"
    :divide   "/"))

;;
;; Calculator State Definition & Transformation
;;

(defn new-state [] {:entry nil, :result nil, :operation nil, :ui-state :zero})

(defn initialize-entry
  [state]
  (assoc state :entry 0))

(defn finalize-entry
  [state]
  (if-let [entry (:entry state)]
    (assoc state :result entry, :entry nil)
    state))

(defn enter-digit
  [state digit]
  (update state :entry #(-> % (* 10) (+ digit))))

(defn enter-first-digit
  [state digit]
  (-> state
    finalize-entry
    initialize-entry
    (enter-digit digit)))

(defn choose-operation
  [state operation-kw]
  (assoc state
         :operation {:kw operation-kw, :fn (key->op operation-kw)}))

(defn calculate
  [state]
  (let [{:keys [result entry]} state
        operation (get-in state [:operation :fn] (fn [_result entry] entry))
        entered-op? (boolean (:operation state))]
    (assoc state
           :result (operation result entry)
           :entry nil
           :operation nil
           :mem (if-not entered-op?
                  {}
                  (assoc (:mem state)
                         :operation (:operation state)
                         :arg entry)))))

(defn re-calculate
  [state]
  (let [{:keys [mem result]} state
        {arg :arg, {op-fn :fn} :operation} mem]
    (assoc state :result (op-fn result arg))))

(defn clear-entry
  [state]
  (assoc state :entry 0))

(defn clear-result
  [state]
  (assoc state :result 0))

;;
;; Calculator Key Events
;;

(defn digit-event?
  [event]
  (= (:kind event) :digit))

(defn operation-event?
  [event]
  (= (:kind event) :operation))

(defn equals-event?
  [event]
  (= (:kind event) :equals))

(defn clear-entry-event?
  [event]
  (= (:kind event) :clear-entry))

(defn clear-all-event?
  [event]
  (= (:kind event) :clear-all))

;;
;; Calculator State Machine
;;

;; These functions below (before the state machine definition) are all
;; compatibility functions between Cypress state machine transition functions
;; (which must always take three arguments: the current app state, the new UI
;; state, and the triggering event) and the calculator state transformation
;; functions we've defined above, which take only one or two arguments and
;; always want concrete values rather than the custom event maps used in the
;; state machine.

(defn track-ui-state
  ([app-state new-ui-state] (track-ui-state app-state new-ui-state nil))
  ([app-state new-ui-state _]
   (assoc app-state :ui-state new-ui-state)))

(defn enter-digit*
  [app-state ui-state event]
  (-> (enter-digit app-state (:value event))
    (track-ui-state ui-state)))

(defn enter-first-digit*
  [app-state ui-state event]
  (-> (enter-first-digit app-state (:value event))
    (track-ui-state ui-state)))

(defn choose-operation*
  [app-state ui-state event]
  (-> (choose-operation app-state (:operation event))
    (track-ui-state ui-state)))

(defn calculate*
  [app-state ui-state _]
  (-> (calculate app-state)
    (track-ui-state ui-state)))

(defn re-calculate*
  [app-state ui-state _]
  (-> (re-calculate app-state)
    (track-ui-state ui-state)))

(defn clear-entry*
  [app-state ui-state _]
  (-> (clear-entry app-state)
    (track-ui-state ui-state)))

(defn clear-result*
  [app-state ui-state _]
  (-> (clear-result app-state)
    (track-ui-state ui-state)))

(def clear-all*
  (constantly (new-state)))

(def calculator
  (-> (sm/blank-state-machine :zero)

    (sm/add-transition :zero :zero equals-event? sm/identity)
    (sm/add-transition :zero :zero clear-entry-event? sm/identity)
    (sm/add-transition :zero :zero clear-all-event? clear-all*)
    (sm/add-transition :zero :1st-arg digit-event? enter-first-digit*)
    (sm/add-transition :zero :choose-operation operation-event? choose-operation*)

    (sm/add-transition :1st-arg :1st-arg digit-event? enter-digit*)
    (sm/add-transition :1st-arg :1st-arg clear-entry-event? clear-entry*)
    (sm/add-transition :1st-arg :choose-operation operation-event? choose-operation*)
    (sm/add-transition :1st-arg :identity equals-event? track-ui-state)
    (sm/add-transition :1st-arg :zero clear-all-event? clear-all*)

    (sm/add-transition :choose-operation :choose-operation operation-event? choose-operation*)
    (sm/add-transition :choose-operation :nth-arg digit-event? enter-first-digit*)
    (sm/add-transition :choose-operation :nth-arg clear-entry-event? clear-entry*)
    (sm/add-transition :choose-operation :result equals-event? calculate*)
    (sm/add-transition :choose-operation :zero clear-all-event? clear-all*)

    (sm/add-transition :nth-arg :nth-arg digit-event? enter-digit*)
    (sm/add-transition :nth-arg :nth-arg clear-entry-event? clear-entry*)
    (sm/add-transition :nth-arg :choose-operation operation-event? choose-operation*)
    (sm/add-transition :nth-arg :result equals-event? calculate*)
    (sm/add-transition :nth-arg :zero clear-all-event? clear-all*)

    (sm/add-transition :result :result equals-event? re-calculate*)
    (sm/add-transition :result :result clear-entry-event? clear-result*)
    (sm/add-transition :result :1st-arg digit-event? enter-digit*)
    (sm/add-transition :result :choose-operation operation-event? choose-operation*)
    (sm/add-transition :result :zero clear-all-event? clear-all*)

    (sm/add-transition :identity :identity equals-event? sm/identity)
    (sm/add-transition :identity :1st-arg digit-event? enter-digit*)
    (sm/add-transition :identity :zero clear-all-event? clear-all*)
    (sm/add-transition :identity :choose-operation operation-event? choose-operation*)))
