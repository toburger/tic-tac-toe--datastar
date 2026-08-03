(ns tic-tac-toe.main
  (:require [tic-tac-toe.server :as server]
            [tic-tac-toe.core :as c]))

(defn -main [& _]
  (let [server (server/start! c/handler)]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn []
                                 (server/stop! server)
                                 (shutdown-agents))))))