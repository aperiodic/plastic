(ns cypress.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [clojure.core.async :refer [>! <! chan take! put!]]
            [clojure.set :as set :refer [subset?]]
            [cypress.state-machine :as sm]))

(defn publish-event!
  [chan kind event]
  (put! chan {:kind kind, :event event}))

(defn event-handler
  [events-chan kind]
  (fn [e]
    (publish-event! events-chan kind e)))


;; For the most part, these are the DOM & touch events that bubble, but one
;; is a special psuedo-event:
;;   * :skip - when a state has a transition that triggers on :skip, it's
;;             followed immediately without waiting for a new event.
(def ^:private event-kind->name
  {:click "click"
   :double-click "dblclick"
   :focus-in "focusin"
   :focus-out "focusout"
   :key-down "keydown"
   :key-up "keyup"
   :key-press "keypress"
   :input "input"
   :mouse-down "mousedown"
   :mouse-move "mousemove"
   :mouse-out "mouseout"
   :mouse-over "mouseover"
   :mouse-up "mouseup"
   :select "select"
   :skip ""
   :touch-cancel "touchcancel"
   :touch-end "touchend"
   :touch-move "touchmove"
   :touch-start "touchstart"
   :wheel "wheel"})

(def ^:private fake-events #{:skip})

(def ^:private event-name->kind
  (into {} (for [[k n] event-kind->name]
             [n k])))

(def supported-event-kinds (set (keys event-kind->name)))
(def supported-event-names (set (vals event-kind->name)))

(defn follow-skips
  "Follows any :skip transition out of the current UI state, and then continues
  transitively until a state without a :skip transition is reached. Returns
  a map with that UI state under :ui and the final app state under :app."
  [ui-state-transitions ui-state app-state]
  (if-let [transition (get-in ui-state-transitions [ui-state :skip])]
    (let [{to :to, app-update :update} transition]
      (recur ui-state-transitions to (app-update app-state)))
    {:app app-state, :ui ui-state}))

(defn transition-target
  "If 'to' is a keyword (i.e. state), return it; if it's a dispatch function,
  call it with the app-state and triggering event to get the dispatched state to
  transition to."
  [to app-state event]
  (if (keyword? to)
    to
    (let [dispatched-to (to app-state event)]
      (if (keyword? dispatched-to)
        dispatched-to
        (throw (js/TypeError.
                 (str "dispatch function " to " triggering on " event " did not"
                      " return a keyword! Instead it returned"
                      " " (pr-str dispatched-to))))))))

(defn event-processor
  [state-machine dom-events !state]
  (let [ui-transitions (sm/unroll-machine state-machine)]
    (go-loop [ui-state (:start state-machine)
              app-state @!state]
      (when-let [{kind :kind, e :event} (<! dom-events)]
        (if-let [{app-update :update :as transition} (get-in ui-transitions
                                                             [ui-state kind])]
          (let [to (transition-target (:to transition) app-state e)
                {app' :app, ui' :ui} (follow-skips
                                       ui-transitions
                                       to
                                       (app-update app-state ui-state e))]
            (reset! !state app')
            (recur ui' app'))
          ; else no transition found
          (recur ui-state app-state))))))

(defn init!
  [dom-event-root ui-state-machine !app-state]
  ;; ensure the state machine is well-formed
  (when-not (sm/valid? ui-state-machine)
    (throw
      (js/TypeError.
        (str "Invalid state machine: " (sm/validation-error ui-state-machine)))))
  ;; ensure the state machine only uses supported events
  (when-not (subset? (sm/events ui-state-machine) supported-event-kinds)
    (let [unsupported (set/difference (sm/events ui-state-machine)
                                      supported-event-kinds)
          plural? (> (count unsupported) 1)]
      (throw
        (js/TypeError.
          (str "Unsupported event "
               (if plural? "types" "type") " "
               (if plural? (seq unsupported) (first unsupported)))))))
  (let [ui-events (chan 16)]
    ;; start the event loop
    (event-processor ui-state-machine ui-events !app-state)
    ;; register a handler publishing to the loop's channel for every kind of
    ;; event that triggers any transition in the ui-state-machine
    (doseq [event-kind (sm/events ui-state-machine)
            :when (not (contains? fake-events event-kind))]
      (let [dom-event-name (event-kind->name event-kind)]
        (.addEventListener dom-event-root
          dom-event-name
          (event-handler ui-events event-kind)
          #js {:passive false})))))
