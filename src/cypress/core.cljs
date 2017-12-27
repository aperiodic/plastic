(ns cypress.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [clojure.core.async :refer [>! <! chan take! put!]]))

(defn publish-event!
  [chan kind page-x page-y]
  (put! chan {:kind kind, :page-x page-x, :page-y page-y}))

(defn event-handler
  [events-chan kind]
  (fn [e]
    (publish-event! events-chan kind (.-pageX e) (.-pageY e))))

(defn event-processor
  [incoming]
  (go-loop []
    (when-let [{:keys [kind page-x page-y]} (<! incoming)]
      (.log js/console "got a" (name kind) "event at (" page-x "," page-y ")")
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
