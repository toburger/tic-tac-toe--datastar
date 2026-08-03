(ns tic-tac-toe.core
  (:require
   [dev.onionpancakes.chassis.core :as h]
   [reitit.ring :as rr]
   [reitit.ring.middleware.parameters :as rmparams]
   [starfederation.datastar.clojure.adapter.ring
    :refer [->sse-response on-close on-open]]
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
  [:div.Content {:id "game"}
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
   [:body
    [:div {:data-init "@get('/stream')"}
     content]]])

(defn render-html [view]
  (h/html (home-page view)))

(defn render-game [game]
  (h/html (game-view game)))

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

(defonce !connections
  (atom #{}))

;; `defonce` preserves the old value during REPL reloads. Migrate the registry
;; created by earlier versions of this namespace, where it was initialized as a map.
(when-not (set? @!connections)
  (reset! !connections #{}))

(defn stream-handler [request]
  (let [closed (promise)]
    (->sse-response
     request
     {on-open
      (fn [sse]
        (swap! !connections conj sse)
        (try
          (d*/patch-elements!
           sse
           (render-game @!game))

          ;; The Ring adapter writes synchronously. Keep this callback alive so
          ;; Jetty does not close the response immediately after the first event.
          @closed
          (finally
            (swap! !connections disj sse))))

      on-close
      (fn [sse]
        (swap! !connections disj sse)
        (deliver closed true))})))

(defn broadcast-game! []
  (let [html (render-game @!game)]
    (doseq [sse @!connections]
      (try
        (d*/patch-elements! sse html)
        (catch Exception _
          (swap! !connections disj sse))))))

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
    (broadcast-game!)
    {:status 204}))

(defn restart-handler [_]
  (reset! !game initial-state)
  (broadcast-game!)
  {:status 204})

(def routes
  [["/" {:handler #'home-handler}]
   ["/move"
    {:post move-handler
     :middleware [rmparams/parameters-middleware]}]
   ["/restart"
    {:post restart-handler}]
   ["/stream" stream-handler]
   ["/css/*" static-resource-handler]
   ["/img/*" static-resource-handler]
   ["/js/*" static-resource-handler]])

(def router (rr/router routes))

(def handler
  (rr/ring-handler
   router
   (rr/create-default-handler)))
