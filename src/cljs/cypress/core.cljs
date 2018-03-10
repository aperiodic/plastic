(ns cypress.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [clojure.core.async :refer [>! <! chan take! put!]]
            [cypress.state-machine :as sm]))

(defn publish-event!
  [chan kind event]
  (put! chan {:kind kind, :event event}))

(defn event-handler
  [events-chan kind]
  (fn [e]
    (publish-event! events-chan kind e)))

(def ^:private event-kind->name
  {:mouse-down "mousedown"
   :mouse-up "mouseup"
   :mouse-move "mousemove"})

(def event-name->kind
  (into {} (for [[k n] event-kind->name]
             [n k])))

(defn- unroll-machine
  "Transform a state machine from the user-facing representation constructed by
  functions in the SM namespace to an equivalent one that's optimized for
  looking up the next state & update action based on the current state & event
  just fired."
  [state-machine]
  (let [sm state-machine
        all-states (sm/states sm)]
    (into {} (for [state all-states]
               [state (into {} (for [t (sm/transitions-from sm state)]
                                 [(:on t) (select-keys t [:to :update])]))]))))

(defn event-processor
  [state-machine incoming]
  (println "unrolling state machine for event processing...")
  (let [state-transitions (unroll-machine state-machine)]
    (println "started go loop with unrolled machine:" state-transitions)
    (go-loop [ui-state (:start state-machine)]
      (when-let [{kind :kind, e :event} (<! incoming)]
        (if-let [{:keys [to update]} (get-in state-transitions [ui-state kind])]
          (do
            (println "found" kind "event, transitioning to" to)
            (recur to))
          ; else (no transition found)
          (recur ui-state))))))

(defn init
  [dom-event-root ui-state-machine]
  (when-not (sm/valid? ui-state-machine)
    (throw
      (js/TypeError.
        (str "Invalid state machine: " (sm/validation-error ui-state-machine)))))
  (let [ui-events (chan 16)]
    (event-processor ui-state-machine ui-events)
    ; register a handler for every supported event type
    (doseq [[event-kind dom-event-name] event-kind->name]
      (.addEventListener dom-event-root
        dom-event-name
        (event-handler ui-events event-kind)
        #js {:passive false}))))
