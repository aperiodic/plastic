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

(defn event-processor
  [incoming]
  (go-loop []
    (when-let [{kind :kind, e :event} (<! incoming)]
      (.log js/console "got a" (name kind) "event at (" (.-pageX e) "," (.-pageY e) ")")
      (recur))))

(def ^:private kind->type
  {:mouse-down "mousedown"
   :mouse-up "mouseup"
   :mouse-move "mousemove"})

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

(defn init
  [dom-event-root ui-state-machine]
  (when-not (sm/valid? ui-state-machine)
    (throw
      (js/TypeError.
        (str "Invalid state machine: " (sm/validation-error ui-state-machine)))))
  (let [ui-events (chan 16)]
    (event-processor ui-events)
    (doseq [[event-kind dom-type] kind->type]
      (.addEventListener dom-event-root
        dom-type
        (event-handler ui-events event-kind)
        #js {:passive false}))))
