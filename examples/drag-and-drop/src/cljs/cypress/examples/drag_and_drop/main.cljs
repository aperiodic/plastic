(ns cypress.examples.drag-and-drop.main
  (:require [cypress.examples.drag-and-drop.core
             :refer [drawing-and-dragging-boxes move-box !state]]
            [cypress.core :as cyp]
            [cypress.state-machine :as sm]
            [om.core :as om]
            [om.dom :as dom]))

;;
;; Utilities
;;

(defn abs
  [n]
  (max n (- n)))

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

(defn render-dragged-boxes
  [{[x_0 y_0] :start, [x y] :current, boxes :boxes}]
  (let [delta-x (- x x_0)
        delta-y (- y y_0)]
    (map render-box (map #(move-box % delta-x delta-y) boxes))))

(defn render-app
  [state]
  (dom/div nil
    (dom/div #js {:className "boxes-wrapper"}
      (map render-box (:boxes state)))
    (dom/div #js {:className "dragged-wrapper"}
      (render-dragged-boxes (:drag state)))
    (if-let [box-being-drawn (:drawing state)]
      (render-box box-being-drawn))))

;;
;; Main & Friends
;;

(defn app-node
  []
  (.getElementById js/document "drag-and-drop"))

(defn start!
  []
  (cyp/init! js/document drawing-and-dragging-boxes !state)
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
