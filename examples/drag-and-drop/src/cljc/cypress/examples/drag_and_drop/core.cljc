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
;; Geometry
;;

(defn in-box?
  [{box-x :x, box-y :y, :keys [width height]} x y]
  (let [[x-min x-max] (sort [box-x, (+ box-x width)])
        [y-min y-max] (sort [box-y, (+ box-y height)])]
    (and (<= x-min x x-max)
         (<= y-min y y-max))))

(defn in-any-box?
  [boxes x y]
  (some #(in-box? % x y) boxes))

(defn move-box
  [{:keys [x y] :as box} delta-x delta-y]
  (-> box
    (update :x + delta-x)
    (update :y + delta-y)))

;;
;; App Logic
;;

(defonce !state (atom {:boxes []
                       :drawing nil
                       :drag nil}))

(defn start-drawing
  [app-state ui-state mouse-down]
  (let [[x y] (pos mouse-down)]
    (assoc app-state :drawing {:x x, :y y, :width 0, :height 0
                               :id #?(:cljs (random-uuid)
                                            :clj (UUID/randomUUID))})))

(defn update-drawing
  [app-state ui-state mouse-move]
  (let [[x y] (pos mouse-move)
        {x_0 :x, y_0 :y} (:drawing app-state)]
    (-> app-state
      (assoc-in [:drawing :width] (- x x_0))
      (assoc-in [:drawing :height] (- y y_0)))))

(defn finish-drawing
  [app-state ui-state mouse-up]
  (let [new-box (:drawing app-state)]
    (-> app-state
      (update :boxes conj new-box)
      (assoc :drawing nil))))

(defn start-drag
  [app-state ui-state mouse-down]
  (let [[x y] (pos mouse-down)]
    (assoc app-state
           :drag {:start [x y]
                  :current [x y]
                  :boxes (filter #(in-box? % x y) (:boxes app-state))}
           :boxes (remove #(in-box? % x y) (:boxes app-state)))))

(defn update-drag
  [app-state ui-state mouse-move]
  (assoc-in app-state [:drag :current] (pos mouse-move)))

(defn finish-drag
  [app-state ui-state mouse-up]
  (let [[x y] (pos mouse-up)
        [x_0, y_0] (get-in app-state [:drag :start])
        delta-x (- x x_0)
        delta-y (- y y_0)
        drag-boxes (get-in app-state [:drag :boxes])]
    (-> app-state
      (update :boxes concat (map #(move-box % delta-x delta-y) drag-boxes))
      (assoc :drag nil))))

(defn delete-last-box
  [app-state ui-state mouse-down]
  (update app-state :boxes (comp vec drop-last)))

(defn dragging-or-drawing
  [app-state mouse-down]
  (let [[x y] (pos mouse-down)]
    (if (in-any-box? (:boxes app-state) x y)
      :dragging
      :drawing)))

(defn start-drag-or-draw
  [app-state ui-state mouse-down]
  (case ui-state
    :drawing (start-drawing app-state ui-state mouse-down)
    :dragging (start-drag app-state ui-state mouse-down)))

(def drawing-and-dragging-boxes
  (-> (sm/blank-state-machine :idle)
    (sm/add-transition :idle :idle :delete-last delete-last-box)
    (sm/add-transition :idle dragging-or-drawing :mouse-down start-drag-or-draw)

    (sm/add-transition :drawing :drawing :mouse-move update-drawing)
    (sm/add-transition :drawing :idle    :mouse-up finish-drawing)

    (sm/add-transition :dragging :dragging :mouse-move update-drag)
    (sm/add-transition :dragging :idle     :mouse-up finish-drag)))
