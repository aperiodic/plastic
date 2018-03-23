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
   :touch-cancel "touchcancel"
   :touch-end "touchend"
   :touch-move "touchmove"
   :touch-start "touchstart"
   :wheel "wheel"})

(def ^:private event-name->kind
  (into {} (for [[k n] event-kind->name]
             [n k])))

(def supported-event-kinds (set (keys event-kind->name)))
(def supported-event-names (set (vals event-kind->name)))

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
    (doseq [event-kind (sm/events ui-state-machine)]
      (let [dom-event-name (event-kind->name event-kind)]
        (.addEventListener dom-event-root
          dom-event-name
          (event-handler ui-events event-kind)
          #js {:passive false})))))
