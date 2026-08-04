(ns tic-tac-toe.views
  (:require
   [dev.onionpancakes.chassis.core :as h]))

(defn player-x [props]
  [:img.PlayerX
   (merge
    {:alt "PlayerX"
     :src "./img/PlayerX.svg"}
    props)])

(defn player-o [props]
  [:img.PlayerO
   (merge
    {:alt "PlayerO"
     :src "./img/PlayerO.svg"}
    props)])

(defn no-player []
  [:span.NoPlayer])

(defn dispatch-player [player]
  [:span
   (case player
     :x (player-x {:class "PlayerX--Small"})
     :o (player-o {:class "PlayerO--Small"})
     (no-player))])

(defn game-cell [x y cell]
  [:button.Cell
   {:disabled (some? cell)
    :data-on:click (format "@post('/move?x=%s&y=%s')" x y)}
   (case cell
     :x (player-x {})
     :o (player-o {})
     (no-player))])

(defn game-board [board]
  [:div.Board
   (map-indexed
    (fn [rowidx row]
      [:div.Board__Row
       {:key rowidx}
       (map-indexed
        (fn [colidx cell]
          ^{:key (str rowidx "-" colidx)}
          (game-cell rowidx colidx cell)) row)])
    board)])

(defn game-current-player [current-player]
  [:div.CurrentPlayer
   [:span.CurrentPlayer__Text
    "Player:"
    (dispatch-player current-player)]])

(defn game-field [board current-player]
  [:div
   (game-board board)
   (game-current-player current-player)])

(def restart-image "./img/restart.png")

(defn game-over [winner]
  [:div.GameOver
   [:img.GameOver__Image
    {:data-on:click "@post('/restart')"
     :src      restart-image
     :alt      "Restart"}]
   [:p.GameOver__Text
    (case winner
      :draw "It's a draw!"
      [:span
       "Player"
       (dispatch-player winner)
       "wins!"])]])

(defn game-view [{:keys [board current-player winner]}]
  [:div.Content {:id "game"}
   [:div.App
    (if winner
      (game-over winner)
      (game-field board current-player))]])

(defn home-page [content]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:link {:rel "icon" :type "image/x-icon" :href "favicon.ico"}]
    [:link {:rel "stylesheet" :href "/css/style.css" :type "text/css"}]
    [:script
     {:type "module"
      :src
      "https://cdn.jsdelivr.net/gh/starfederation/datastar@main/bundles/datastar.js"}]
    [:title "Tic Tac Toe - Datastar"]]
   [:body
    [:div {:data-init "@get('/stream')"}
     content]]])

(defn render-page [game]
  (h/html (home-page (game-view game))))

(defn render-game [game]
  (h/html (game-view game)))