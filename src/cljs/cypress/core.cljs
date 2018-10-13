(ns cypress.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async]
            [clojure.core.async :refer [>! <! chan take! put!]]
            [clojure.set :as set :refer [subset?]]
            [cypress.state-machine :as sm]))

(defn publish-event!
  [chan kind event]
  (put! chan {:kind kind, :event event}))

(defn event-handler
  [events-chan kind]
  (fn [e]
    (publish-event! events-chan kind e)))

;; For the most part, cypress's events are the DOM & touch events that bubble,
;; but there is a special event that doesn't correspond to an HTML event:
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
   :touch-cancel "touchcancel"
   :touch-end "touchend"
   :touch-move "touchmove"
   :touch-start "touchstart"
   :wheel "wheel"})

(def ^:private event-name->kind
  (into {} (for [[k n] event-kind->name]
             [n k])))

(defn- follow-skips
  "Follows any :skip transition out of the current UI state, and then continues
  transitively until a state without a :skip transition is reached. Returns
  a map with that UI state under :ui and the final app state under :app."
  [ui-state-transitions ui-state app-state]
  (if-let [transition (get-in ui-state-transitions [ui-state :skip])]
    (let [{to :to, app-update :update} transition]
      (recur ui-state-transitions to (app-update app-state to :skip)))
    {:app app-state, :ui ui-state}))

(defn- transition-target
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

(defn- dest-descr
  [{to :to, :as transition}]
  (if (keyword? to)
    (str "to " to)
    "with dispatched destination"))

(defn- triggered-transition
  "Given an unrolled state machine's transition map for a particular state and
  a triggering event, return the transition triggered by the event (if any), and
  nil otherwise."
  [state-transitions {::keys [event kind]} logging?]
  (or (get state-transitions kind)
      (if-let [custom-triggered (:cypress/event-recognizers state-transitions)]
        (do
          (when logging?
            (println "Looking for a recognition function that fires on this"
                     kind "event"))
          (loop [transitions custom-triggered]
            (if (empty? transitions)
              (when logging?
                (println "No recognition function fired on the" kind "event"))
              (let [{should-fire? :on, :as transition} (first transitions)]
                (if (should-fire? event)
                  (do (when logging?
                        (println "Transition" (dest-descr transition)
                                 "fired on the" kind "event"))
                    transition)
                  (recur (next transitions))))))))))

(defn event-processor
  [state-machine events !state logging?]
  (let [ui-transitions (sm/unroll-machine state-machine)]
    (go-loop [ui-state (:start state-machine)
              app-state @!state]
      (when-let [{:keys [event kind], :as wrapped-event} (<! events)]
        (when logging?
          (println "Found a" kind "event" (str (pr-str event) ",")
                   "full log on next line")
          (.log js/console event))
        (if-let [t (triggered-transition (get ui-transitions ui-state)
                                         wrapped-event
                                         logging?)]
          (do
            (when logging?
              (println "Found transition from" ui-state "on" kind))
            (let [{app-update :update :as transition} t
                  to (transition-target (:to transition) app-state event)
                  _ (when logging?
                      (when (fn? (:to transition))
                        (println "Called dispatch fn to determine next state"))
                      (println "Transitioning to next state" to))
                  {app' :app, ui' :ui} (follow-skips
                                         ui-transitions
                                         to
                                         (app-update app-state to event))]
              (when logging?
                (println "Transition complete, now in UI state" ui' "with new app state")
                (println))
              (reset! !state app')
              (recur ui' app')))
          ; else no transition found
          (recur ui-state app-state))))))

(defn- custom-event-processor
  [kind]
  (fn [e]
    {:kind kind, :event e}))

(def init!-default-opts
  {:logging false})

(defn init!
  ([dom-event-roots ui-state-machine !app-state]
   (init! dom-event-roots {} ui-state-machine !app-state init!-default-opts))
  ([dom-event-roots custom-event-chans ui-state-machine !app-state]
   (init! dom-event-roots custom-event-chans ui-state-machine !app-state
          init!-default-opts))
  ([dom-event-roots custom-event-chans ui-state-machine !app-state opts]
   ;; ensure the state machine is well-formed
   (when-not (sm/valid? ui-state-machine)
     (throw
       (js/TypeError.
         (str "Invalid state machine: " (sm/validation-error ui-state-machine)))))
   (let [{:keys [logging]} (merge init!-default-opts opts)
         ui-events (chan 16)
         dom-nodes (if (vector? dom-event-roots)
                     dom-event-roots
                     [dom-event-roots])
         all-events (-> (for [[kind channel] custom-event-chans]
                          (let [->cypress-event (custom-event-processor kind)]
                            (async/pipe channel
                                        (async/chan 4 (map ->cypress-event)))))
                      (conj ui-events)
                      (async/merge))]
     ;; start the event loop
     (event-processor ui-state-machine all-events !app-state logging)
     ;; register a handler publishing to the loop's channel for every kind of
     ;; event that triggers any transition in the ui-state-machine
     (doseq [dom-node dom-nodes
             event-kind (sm/events ui-state-machine)
             :when (contains? event-kind->name event-kind)]
       (.addEventListener dom-node
         (event-kind->name event-kind)
         (event-handler ui-events event-kind)
         #js {:passive false})))))
