(ns me.bartleby.space-invaders.core)

;; various game states
; 1. position of player
; 2. position of enemy sprites
; 3. count of enemy sprites

; 4. game started/stopped
(defonce game-state (atom {:running? false
                           :player {:position {:x 250 :y 350}}
                           :enemies {:count 0
                                     :position {:x nil :y nil}}}))

;; helper functions for accessing game state
(defn get-player-x [st]
  (get-in @st [:player :position :x]))

(defn get-player-y [st]
  (get-in @st [:player :position :y]))

(defn get-enemy-count [st]
  (get-in @st [:enemies :count]))

(defn get-enemy-pos
  "Enemy sprites will appear in groups (rows) so their \"position\" is represented
   as that of the top left sprite and bottom right sprite."
  [st]
  (get-in @st [:enemies :position]))

;; drawing functions

; player-start position
(defn draw-player [game-state ^js ctx]
  ; only call this on app init
  (let [[player-x player-y] ((juxt get-player-x get-player-y) game-state)
        img (js/Image.)]
    (js/console.log player-x player-y)
    (set! (.-src img) "assets/player.png")
    (set! (.-onload img)
          (fn []
            (.drawImage ctx img player-x player-y)))))

;; TODO:
; 1. get canvas and context
(defonce canvas (.getElementById js/document "game"))
(defonce ctx (.getContext canvas "2d"))
; 2. position player at center and bottom

;; TODO:
; 1. make a start button
; 2. create event handler for starting game when button is clicked

;; TODO:

(defn ^:dev/after-load init []
  (js/console.log "loaded")
  #_(draw-dummy ctx)
  (draw-player game-state ctx))