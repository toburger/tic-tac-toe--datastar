(ns tic-tac-toe.handlers
  (:require
   [tic-tac-toe.game :as game]
   [tic-tac-toe.sse :as sse]
   [tic-tac-toe.views :as views]))

(defonce !game (atom game/initial-state))

(defn home-handler [_]
  {:body (views/render-page @!game)
   :status 200})

(defn stream-handler [request]
  (sse/stream-response
   request
   (views/render-game @!game)))

(defn move-handler [request]
  (let [x (parse-long (get-in request [:query-params "x"]))
        y (parse-long (get-in request [:query-params "y"]))]
    (swap! !game game/move x y)
    (sse/broadcast! (views/render-game @!game))
    {:status 204}))

(defn restart-handler [_]
  (reset! !game game/initial-state)
  (sse/broadcast! (views/render-game @!game))
  {:status 204})
