(ns tic-tac-toe.main
  (:require [tic-tac-toe.server :as server]
            [tic-tac-toe.routes :as routes]))

(defn -main [& _]
  (let [server (server/start! routes/handler)]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn []
                                 (server/stop! server)
                                 (shutdown-agents))))))