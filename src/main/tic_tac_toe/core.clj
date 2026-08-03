(ns tic-tac-toe.core
  (:require
   [dev.onionpancakes.chassis.core :as h]
   [reitit.ring :as rr]
   [reitit.ring.middleware.parameters :as rmparams]
   [starfederation.datastar.clojure.adapter.ring
    :refer [->sse-response on-open]]
   [starfederation.datastar.clojure.api :as d*]
   [tic-tac-toe.game-logic :as game-logic]))

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
  [:div.Content {:id "game"
                 :data-on-interval "@get('/refresh')"}
   [:div.App
    (if winner
      (game-over winner)
      (game-field board current-player))]])

(def initial-state
  {:board [[nil nil nil]
           [nil nil nil]
           [nil nil nil]]
   :current-player :x
   :winner nil})

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
   [:body content]])

(defn render-html [view]
  (h/html (home-page view)))

(defonce !game (atom initial-state))

(defn home-handler [_]
  {:body (render-html (game-view @!game))
   :status 200})

(def static-resource-handler
  (rr/create-resource-handler
   {:path ""
    :not-found-handler (fn [{:keys [uri]}]
                         {:status 404
                          :body (str "resource not found: " uri)})}))

(defn patch-game [request]
  (->sse-response
   request
   {on-open
    (fn [sse]
      (d*/with-open-sse sse
        (d*/patch-elements!
         sse
         (render-html (game-view @!game)))))}))

(defn refresh-handler [_]
  (patch-game @!game))

(defn play-move [{:keys [board current-player] :as state} x y]
  (if (game-logic/can-update-cell? board x y)
    (let [new-board (game-logic/update-board board x y current-player)
          new-winner (game-logic/get-winner new-board)
          new-current-player (game-logic/switch-player current-player)]
      {:board new-board
       :current-player new-current-player
       :winner new-winner})
    state))

(defn move-handler [request]
  (let [x (parse-long (get-in request [:query-params "x"]))
        y (parse-long (get-in request [:query-params "y"]))]
    (swap! !game play-move x y)
    (patch-game request)))

(defn restart-handler [request]
  (reset! !game initial-state)
  (patch-game request))

(def routes
  [["/" {:handler #'home-handler}]
   ["/move"
    {:post move-handler
     :middleware [rmparams/parameters-middleware]}]
   ["/restart"
    {:post restart-handler}]
   ["/refresh" refresh-handler]
   ["/css/*" static-resource-handler]
   ["/img/*" static-resource-handler]
   ["/js/*" static-resource-handler]])

(def router (rr/router routes))

(def handler
  (rr/ring-handler
   router
   (rr/create-default-handler)))