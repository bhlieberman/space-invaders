(ns me.bartleby.space-invaders.core
  (:require
   [goog.async.nextTick]
   [goog.dom.xml :as xml]
   [goog.events :refer [listen]]))

;; various game states
;; 1. position of player
;; 2. position of enemy sprites
;; 3. count of enemy sprites

(def enemy-width
  56)

(def enemy-height
  43)

;; 4. game started/stopped
(defonce game-state (atom {:running? false
                           :player {:position {:x 550 :y 700}}
                           :enemies-tick {:dx 1 :dy 10}
                           :enemies (for [x (range 0 6)
                                          y (range 0 2)]
                                      {:x (* x enemy-width)
                                       :y (* y enemy-height)})}))

;; helper functions for accessing game state
(defn get-player-x [st]
  (get-in @st [:player :position :x]))

(defn set-player-pos [st new-x]
  (swap! st update-in [:player :position :x] #(- % new-x)))

(defn get-player-y [st]
  (get-in @st [:player :position :y]))

(defn get-enemy-tick [st]
  (:enemies-tick @st))

(defn get-enemies
  [st]
  (:enemies @st))


;; get canvas and context
(defonce canvas (.getElementById js/document "game"))
(defonce ctx (.getContext canvas "2d"))

(defn any-right?
  [st dx]
  (some #(<= (.-width canvas) (+ (:x %) dx enemy-width))
    (get-enemies st)))

(defn any-left?
  [st dx]
  (some #(<= (+ (:x %) dx) 0)
    (get-enemies st)))

(defn move-horizontal
  [st dx]
  (swap! st update :enemies
    (fn [enemies]
      (for [{:keys [x y]} enemies]
        {:x (+ x dx) :y y}))))

(defn move-vertical
  [st dy]
  (swap! st update :enemies
    (fn [enemies]
      (for [{:keys [x y]} enemies]
        {:x x :y (+ y dy)}))))

;; position player at center and bottom
(defn draw-player [game-state ^js ctx]
  (let [[player-x player-y] ((juxt get-player-x get-player-y) game-state)
        sprite (.-player-sprite js/window)]
    (.drawImage ctx sprite player-x player-y)))

(defn draw-enemies [game-state ^js ctx]
  ;; 1. check game state -- if not started, there should be no sprites
  ;; 1a. if not running, set the enemy top-left and bottom-right maps
  (let [sprite (.-enemy-sprite js/window)]
    (doseq [{:keys [x y]} (get-enemies game-state)]
      (.drawImage ctx sprite x y))
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (goog.async.nextTick
     (fn []
       (let [{:keys [dx dy]} (:enemies-tick @game-state)]
        ;; check position of top-left and bottom-right corners
        (when (or (any-left? game-state dx) (any-right? game-state dx))
          (doto game-state
            (swap! update-in [:enemies-tick :dx] * -1)
            (move-vertical dy)))
        (move-horizontal game-state dx))))))

(comment @game-state)

(defn draw [game-state ^js ctx]
  (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))
  (draw-player game-state ctx)
  (draw-enemies game-state ctx)
  (js/requestAnimationFrame #(draw game-state ctx)))

;; functions to update player-position on keydown and keyup
(defn check-key
  "Auxiliary function that returns the delta position of the player
   based on the key pressed"
  [e]
  (case (.-key e)
    "ArrowLeft" 10
    "ArrowRight" -10
    0))

(defn add-key-listeners []
  ;; add event listeners for moving player
  (let [key-down-handler (fn [e]
                           (let [new-x (check-key e)]
                             (set-player-pos game-state new-x)))]
    (listen js/document "keydown" key-down-handler)))

(defn load-game-images
  "Loads the sprites for display when the game starts."
  []
  (let [player-img (js/Image.)
        enemy-img (js/Image.)]
    (xml/setAttributes player-img #js {:src "assets/player.png"
                                       :id "p-sprite"})
    (set! (.-player-sprite js/window) player-img)
    (xml/setAttributes enemy-img #js {:src "assets/enemy-sprite-small.png"
                                      :class "e-sprite"})
    (set! (.-enemy-sprite js/window) enemy-img)))

(defn ^:dev/after-load init []
  (js/console.log "loaded"))

(defn show-modal []
  (let [Modal (.. js/window -bootstrap -Modal)
        modal-el (.getElementById js/document "startModal")
        modal (new Modal "#startModal" #js {})
        modal-button (.getElementById js/document "startButton")
        click-handler (fn [_]
                        (swap! game-state update :running? not)
                        (draw game-state ctx)
                        (js/requestAnimationFrame #(draw game-state ctx))
                        (.hide modal))]
    (.addEventListener modal-el "show.bs.modal"
                       (fn []
                         (listen modal-button "click" click-handler)))
    (.show modal)))

(defn ^:export main []
  (add-key-listeners)
  (show-modal)
  (load-game-images))

(comment
  (init)


  (main)
  )
