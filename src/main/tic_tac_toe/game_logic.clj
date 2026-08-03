(ns tic-tac-toe.game-logic)

(defn switch-player
  "Switches a player to the next player."
  [player]
  (case player
    :x :o
    :o :x))

(defn update-board
  "Updates a single `board` value by `x` and `y` coordinates and returns the modified board.
  Leaves the original board unchanged."
  [board x y value]
  (assoc-in board [x y] value))

(defn get-cell
  "Gets a single cell value in a `board` by providing `x` and `y` coordinates"
  [board x y]
  (get-in board [x y]))

(defn can-update-cell?
  "Checks if a cell value can be updated.
  (simply checks if the `board` contains nil)"
  [board x y]
  (nil? (get-cell board x y)))

(defn- check? [v row] (every? (partial = v) row))

(defn winner-in-rows?
  "Checks if all elements a row contain the same value `v`.
  Checks all three rows."
  [board v]
  (not (every? false? (map (partial check? v) board))))

(defn- transpose [m]
  (apply mapv vector m))

(defn winner-in-cols?
  "Checks if all elements of a column contain the same value `v`.
  Checks all three columns.

  Uses a simple trick by transposing the board so that the whole board
  gets rotated and the columns become rows. Then the rows can be passed
  to checkRowsForWinner"
  [board v]
  (winner-in-rows? (transpose board) v))

(defn winner-in-diagonals?
  "Checks if all elements of a diagonal contain the same value `v`.
  Checks the two diagonals."
  [board v]
  (let [get       (partial get-in board)
        diagonals [[[0 0] [1 1] [2 2]]
                   [[2 0] [1 1] [0 2]]]]
    (->> diagonals
         (map (comp (partial check? v)
                    (partial map get)))
         (every? false?)
         not)))

(defn winner?
  "Checks if any one of the
  * rows
  * columns
  * diagonals
  of the `board` contain the same value `v`."
  [board v]
  (or (winner-in-rows? board v)
      (winner-in-cols? board v)
      (winner-in-diagonals? board v)))

(defn draw?
  "Checks if the game is a draw
  It does check if all of the boards aren't nil."
  [board]
  (every? some? (flatten board)))

(defn get-winner
  "Determines either the winner or a draw.
  If null is returned the game continues."
  [board]
  (cond
    (winner? board :x)
    :x
    (winner? board :o)
    :o
    (draw? board)
    :draw
    :else
    nil))

(comment
  (def board [[:x nil :o] [nil nil nil] [:x :x :x]])

  (prn board)

  (update-board board 1 1 :x)

  (can-update-cell? board 0 0)
  (can-update-cell? board 0 1)

  (winner-in-rows? board :x)
  (winner-in-cols? board :x)

  (winner? board :x)
  (winner? board :o)

  (draw? board)
  (draw? [[nil nil nil] [nil nil nil] [nil nil nil]])

  (get-winner board)
  (get-winner [[nil nil :o] [nil nil nil] [nil nil nil]])
  (get-winner [[nil nil nil] [nil nil nil] [nil nil nil]]))