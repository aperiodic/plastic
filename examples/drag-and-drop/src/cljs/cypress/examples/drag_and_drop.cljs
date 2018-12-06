(ns cypress.examples.drag-and-drop
  (:require [cypress.core :as cyp]
            [cypress.state-machine :as sm]
            [om.core :as om]
            [om.dom :as dom]))

;;
;; Utilities
;;

(defn abs
  [n]
  (max n (- n)))

(defn pos
  "Convert a mouse event to a vector of the client X & Y coordinates."
  [mouse-event]
  [(.-clientX mouse-event) (.-clientY mouse-event)])

;;
;; App Logic
;;

(defonce !state (atom {:boxes []
                       :drag nil}))

(defn start-dragging
  [app-state ui-state mouse-down]
  (let [[x y] (pos mouse-down)]
    (assoc app-state :drag {:x x, :y y, :width 0, :height 0, :id (random-uuid)})))

(defn update-drag
  [app-state ui-state mouse-move]
  (let [[x y] (pos mouse-move)
        {x_0 :x, y_0 :y} (:drag app-state)]
    (-> app-state
      (assoc-in [:drag :width] (- x x_0))
      (assoc-in [:drag :height] (- y y_0)))))

(defn finish-dragging
  [app-state ui-state mouse-up]
  (let [new-box (:drag app-state)]
    (-> app-state
      (update :boxes conj new-box)
      (assoc :drag nil))))

(def drag-to-draw-a-box
  (-> (sm/blank-state-machine :idle)
    (sm/add-transition :idle     :dragging :mouse-down start-dragging)
    (sm/add-transition :dragging :dragging :mouse-move update-drag)
    (sm/add-transition :dragging :idle     :mouse-up finish-dragging)))

;;
;; Rendering
;;

(defn render-box
  [{:keys [x y width height id]}]
  (let [left (if (pos? width) x (+ x width))
        top (if (pos? height) y (+ y height))]
    (dom/div #js {:className "box"
                  :key id
                  :style #js {:left left
                              :top top
                              :width (abs width)
                              :height (abs height)}})))

(defn render-app
  [state]
  (dom/div nil
    (dom/div #js {:className "boxes-wrapper"}
      (map render-box (:boxes state)))
    (if-let [drag-box (:drag state)]
      (render-box drag-box))))

;;
;; Main & Friends
;;

(defn app-node
  []
  (.getElementById js/document "drag-and-drop"))

(defn start!
  []
  (cyp/init! js/document drag-to-draw-a-box !state)
  (om/root
    (fn [state _owner]
      (reify
        om/IRender
        (render [_]
          (render-app state))))
    !state
    {:target (app-node)}))

(defn clear-app-node!
  "Remove all the event handlers registered on the app's base DOM node by
  replacing it with a clone. This is only necessary when using figwheel because
  it reloads the namespace without refreshing the DOM."
  []
  (let [old-node (app-node)]
    (.replaceChild (.-parentNode old-node) (.cloneNode old-node true) old-node)))

(enable-console-print!)
(clear-app-node!)
(start!)
