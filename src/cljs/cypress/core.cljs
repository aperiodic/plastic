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


(defn event-processor
  [state-machine dom-events !state]
  (let [state-transitions (sm/unroll-machine state-machine)]
    (go-loop [ui-state (:start state-machine)
              app-state @!state]
      (when-let [{kind :kind, e :event} (<! dom-events)]
        (if-let [{to :to, app-update :update} (get-in state-transitions [ui-state kind])]
          (let [app-state' (app-update app-state ui-state e)]
            (reset! !state app-state')
            (recur to app-state'))
          ; else (no transition found)
          (recur ui-state app-state))))))

(defn init!
  [dom-event-root ui-state-machine !app-state]
  (when-not (sm/valid? ui-state-machine)
    (throw
      (js/TypeError.
        (str "Invalid state machine: " (sm/validation-error ui-state-machine)))))
  (let [ui-events (chan 16)]
    (event-processor ui-state-machine ui-events !app-state)
    ;; register a handler for every supported event type
    (doseq [[event-kind dom-event-name] event-kind->name]
      (.addEventListener dom-event-root
        dom-event-name
        (event-handler ui-events event-kind)
        #js {:passive false}))))
