(ns cypress.examples.click-game
  (:require [cypress.core :as cyp]
            [cypress.state-machine :as sm]
            [om.core :as om]
            [om.dom :as dom]))

;;
;; Utilities
;;

(defn irand
  ([hi] (irand 0 hi))
  ([lo hi]
   (int (-> (rand (- hi lo))
          (+ lo)))))

(defn distance
  [x0 y0 x1 y1]
  (let [xd (- x1 x0)
        yd (- y1 y0)]
    (Math/sqrt (+ (* xd xd) (* yd yd)))))

;;
;; Game State Creation
;;

(defn rand-target
  [max-x max-y max-r]
  {:pos [(irand max-x) (irand max-y)]
   :r (irand 5 max-r)})

(defn rand-targets
  [n max-x max-y max-r]
  (take n (repeatedly #(rand-target max-x max-y max-r))))

(def w 900)
(def h 900)
(def widest-target 30)
(def num-targets 5)

(defn new-state
  [win-at]
  {:targets (rand-targets num-targets w h widest-target)
   :status :playing})

(defonce !state (atom (new-state num-targets)))

;;
;; Game Logic
;;

(declare wrapper-node)

(defn in-target?
  [x y target]
  (let [[tx ty] (:pos target)]
    (<= (distance tx ty x y)
       (:r target))))

(defn target-hit?
  [targets click-x click-y]
  (-> (some (partial in-target? click-x click-y) targets)
    boolean))

(defn event->pos
  [mouse-event]
  (let [root-pos (.getBoundingClientRect (wrapper-node))]
    [(- (.-clientX mouse-event) (.-left root-pos))
     (- (.-clientY mouse-event) (.-top root-pos))]))

(defn hit-or-miss
  [app-state mouse-down]
  (let [[mx my] (event->pos mouse-down)
        hit? (target-hit? (:targets app-state) mx my)
        one-target? (= 1 (count (:targets app-state)))]
    (cond
      (not hit?) :lost
      (and hit? one-target?) :won
      :else-hit :playing)))

(defn advance-and-track-status
  [app-state ui-state mouse-down]
  (let [[x y] (event->pos mouse-down)
        without-hit-target (->> (:targets app-state)
                             (remove (partial in-target? x y)))]
    (-> app-state
      (assoc :targets without-hit-target)
      (assoc :status ui-state))))

(defn new-game
  [_ _ _]
  (new-state num-targets))

(def target-hitting-game
  (-> (sm/blank-state-machine :playing)
    (sm/add-transition :playing hit-or-miss :mouse-down advance-and-track-status)
    (sm/add-transition :lost :playing :mouse-down new-game)
    (sm/add-transition :won :playing :mouse-down new-game)))

;;
;; Render State to DOM w/Om
;;

(defn render-loss
  [state]
  (dom/div #js {:className "game-lost target-game"}
    (dom/div #js {:className "game-message target-game"}
      "You Lose 👎")
    (dom/div #js {:className "game-sub-message target-game"}
      "Click to Try Again")))

(defn render-win
  [state]
  (dom/div #js {:className "game-won target-game"}
    (dom/div #js {:className "game-message target-game"}
      "You Win! 🎉")
    (dom/div #js {:className "game-sub-message target-game"}
      "Click to Play Again")))

(defn render-targets
  [targets]
  (dom/div #js {:className "targets-container target-game"}
    (for [[{[tx ty] :pos, r :r} i] (map vector targets (range))]
      (let [x-min (- tx r)
            y-min (- ty r)]
        (dom/div #js {:className "target"
                      :key (str tx "-" ty "-" r "-" i)
                      :style #js {:left x-min
                                  :top y-min
                                  :width (* 2 r)
                                  :height (* 2 r)
                                  :border-radius r}})))))

(defn render-score
  [state]
  (let [{:keys [targets]} state]
    (dom/div #js {:className "scoreboard target-game"}
      (dom/span #js {:className "score target-game"}
        (str (- num-targets (count targets))))
      "/"
      (dom/span #js {:className "goal target-game"}
        (str num-targets)))))

(defn render-round
  [state]
  (dom/div nil
    (render-score state)
    (render-targets (:targets state))))

(defn render-game
  [state]
  ;; This root node is the 'wrapper-node', so that fn should look for this id
  (dom/div #js {:id "board-0"
                :className "target-game game-wrapper"}
    (case (:status state)
      :won (render-win state)
      :lost (render-loss state)
      :playing (render-round state))))

;;
;; DOM Hookup
;;

(defn app-node
  []
  (.getElementById js/document "game"))

(defn wrapper-node
  "Find the root node produced by `render-game`."
  []
  (.getElementById js/document "board-0"))

;;
;; Main Definition and Dev Cleanup
;;

(defn start!
  []
  (cyp/init! (app-node) [] target-hitting-game !state {:logging true})
  (om/root
    (fn [state _owner]
      (reify
        om/IRender
        (render [_]
          (render-game state))))
    !state
    {:target (app-node)}))

(defn clear-app-node!
  "Remove all the event handlers registered on the app's base DOM node by
  replacing it with a clone. This is only necessary when using figwheel because
  it reloads the namespace without refreshing the DOM."
  []
  (let [old-node (app-node)]
    (.replaceChild (.-parentNode old-node) (.cloneNode old-node true) old-node)))

;;
;; Main: Run the App
;;

(enable-console-print!)
(clear-app-node!)
(start!)
