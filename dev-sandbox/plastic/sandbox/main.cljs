(ns plastic.sandbox.main
  (:require [cljs.core.async :as async]
            [plastic.core :as plastic]
            [plastic.state-machine :as sm]
            [om.core :as om]
            [om.dom :as dom]))

(defonce !state (atom {:ups 0, :downs 0}))

(defn sandbox-node
  []
  (.getElementById js/document "sandbox"))

(defn sibling-node
  []
  (.getElementById js/document "sibling"))

(defn element-event-channel
  [element event-name tag]
  (let [out (async/chan 4)]
    (.addEventListener element event-name
      (fn [_]
        (println "emitting a" tag "on" event-name)
        (async/put! out tag))
      #js {:passive false})
    out))

(defn sandbox-down-channel
  []
  (element-event-channel (sandbox-node) "mousedown" :down))

(defn sandbox-up-channel
  []
  (element-event-channel (sandbox-node) "mouseup" :up))

(defn clear-sandbox!
  []
  (let [sandbox (sandbox-node)
        new-sandbox (.cloneNode sandbox)]
    (.replaceChild (.-parentNode sandbox) new-sandbox sandbox)))

(defn add-down
  [app-state ui-state triggering-event]
  (update app-state :downs inc))

(defn add-up
  [app-state ui-state triggering-event]
  (update app-state :ups inc))

(def click-unclick
  (-> (sm/blank-state-machine :idle)
    (sm/add-transition :idle :active :mouse-down add-down)
    (sm/add-transition :active :idle :mouse-up add-up)
    (sm/add-transition :idle :active ::down add-down)
    (sm/add-transition :active :idle ::up add-up)))

(defn start!
  []
  (let [custom-event-sources {::down (sandbox-down-channel),
                              ::up (sandbox-up-channel)}]
    (plastic/init! (sibling-node) custom-event-sources click-unclick !state
                   {:logging true}))
  (om/root
    (fn [state _owner]
      (reify
        om/IRender
        (render [_]
          (dom/div nil
            (dom/div #js {:id "contained"})
            (dom/p nil
              (str "↓:" (:downs state)
                   " ↑:" (:ups state) ))))))
    !state
    {:target (sandbox-node)}))

(defn -main
  [& _]
  (enable-console-print!)
  (clear-sandbox!)
  (start!))

(-main)
