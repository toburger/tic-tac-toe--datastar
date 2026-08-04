(ns tic-tac-toe.game
  (:require
   [tic-tac-toe.game-logic :as game-logic]))

(def initial-state
  {:board [[nil nil nil]
           [nil nil nil]
           [nil nil nil]]
   :current-player :x
   :winner nil})

(defn move [{:keys [board current-player] :as state} x y]
  (if (game-logic/can-update-cell? board x y)
    (let [new-board (game-logic/update-board board x y current-player)
          new-winner (game-logic/get-winner new-board)
          new-current-player (game-logic/switch-player current-player)]
      {:board new-board
       :current-player new-current-player
       :winner new-winner})
    state))
