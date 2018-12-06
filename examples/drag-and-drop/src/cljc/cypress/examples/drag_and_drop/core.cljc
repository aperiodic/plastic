(ns cypress.examples.drag-and-drop.core
  (:require [cypress.state-machine :as sm]
            [om.core :as om]
            [om.dom :as dom])
  #?(:clj (:import java.util.UUID)))

;;
;; Utilities
;;

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
    (assoc app-state :drag {:x x, :y y, :width 0, :height 0
                            :id #?(:cljs (random-uuid)
                                   :clj (UUID/randomUUID))})))

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
