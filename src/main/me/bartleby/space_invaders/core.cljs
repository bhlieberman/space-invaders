(ns me.bartleby.space-invaders.core
  (:require
   [goog.async.nextTick]
   [goog.dom :as gdom]
   [goog.dom.xml :as xml]
   [goog.events :refer [listen]]
   [goog.functions :refer [debounce]]))

;; various game states
; 1. position of player
; 2. position of enemy sprites
; 3. count of enemy sprites

; 4. game started/stopped
(defonce game-state (atom {:running? false
                           :player {:position {:x 250 :y 350}}
                           :enemies-tick {:dx 10 :dy 10}
                           :enemies {:count 0
                                     :position {:top-left {:lx 20 :ly 5}
                                                :bottom-right {:rx 300 :ry 100}}}}))

;; helper functions for accessing game state
(defn get-player-x [st]
  (get-in @st [:player :position :x]))

(defn set-player-pos [st new-x]
  (swap! st update-in [:player :position :x] #(- % new-x)))

(defn get-player-y [st]
  (get-in @st [:player :position :y]))

(defn get-enemy-tick [st]
  (:enemies-tick @st))

(defn get-enemy-pos
  "Enemy sprites will appear in groups (rows) so their \"position\" is represented
   as that of the top left sprite and bottom right sprite."
  [st]
  (get-in @st [:enemies :position]))


;; get canvas and context
(defonce canvas (.getElementById js/document "game"))
(defonce ctx (.getContext canvas "2d"))

;; position player at center and bottom
(defn draw-player [game-state ^js ctx]
  (let [[player-x player-y] ((juxt get-player-x get-player-y) game-state)
        sprite (.-player-sprite js/window)]
    (.drawImage ctx sprite player-x player-y)))

(defn slow-log [o] (debounce #(js/console.log o) 5000))

(defn draw-enemies [game-state ^js ctx]
  ;; 1. check game state -- if not started, there should be no sprites
  ;; 1a. if not running, set the enemy top-left and bottom-right maps
  (let [{:keys [top-left bottom-right]} (get-enemy-pos game-state)
        [left right] [(:lx top-left) (:rx bottom-right)]
        [up down] [(:ly top-left) (:ry bottom-right)]
        sprite (.-enemy-sprite js/window)]
    (doseq [x (range left right 50)
            y (range up down 50)]
      (.drawImage ctx sprite x y))
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (goog.async.nextTick
     (fn []
       ;; check position of top-left and bottom-right corners
       (let [{:keys [dx dy]} (get-enemy-tick game-state)
             {:keys [top-left bottom-right]} (get-enemy-pos game-state)
             {:keys [lx]} top-left
             {:keys [rx]} bottom-right]
         ;; if lx + dx < 0, turn around!!!
         ;; which means invert dx
         ;; or if rx + dx > canvas width
         ;; also turn around
         ;; should they be checked together?
         (.clearRect ctx 0 0 top-left bottom-right)
         (doto game-state
           (swap! update-in [:enemies :position :bottom-right]
                  (fn [m] (if (< (+ rx dx) (.-width canvas))
                            (update m :rx + dx)
                            (update m :rx - dx))))
           (swap! update-in [:enemies :position :top-left]
                  (fn [m] (if (< 0 (+ lx dx))
                            (update m :lx - dx)
                            (update m :lx + dx))))))))))

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
  (let [key-up-handler (fn [e]
                         (let [new-x (check-key e)]
                           (set-player-pos game-state new-x)))
        key-down-handler (fn [e]
                           (let [new-x (check-key e)]
                             new-x))]
    (doto js/document
      (listen "keyup" key-up-handler)
      (listen "keydown" key-down-handler))))

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
  (js/console.log "loaded")
  (add-key-listeners))

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
  (show-modal)
  (load-game-images))

(comment (main))
