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

(defn init
  [dom-event-root]
  (let [ui-events (chan 16)]
    (event-processor ui-events)
    (doseq [[event-kind dom-type] kind->type]
      (.addEventListener dom-event-root
        dom-type
        (event-handler ui-events event-kind)
        #js {:passive false}))))
