(ns tic-tac-toe.routes
  (:require
   [reitit.ring :as rr]
   [reitit.ring.middleware.parameters :as rmparams]
   [tic-tac-toe.handlers :as handlers]))

(def static-resource-handler
  (rr/create-resource-handler
   {:path ""
    :not-found-handler (fn [{:keys [uri]}]
                         {:status 404
                          :body (str "resource not found: " uri)})}))

(def routes
  [["/" {:handler handlers/home-handler}]
   ["/move"
    {:post handlers/move-handler
     :middleware [rmparams/parameters-middleware]}]
   ["/restart"
    {:post handlers/restart-handler}]
   ["/stream" handlers/stream-handler]
   ["/css/*" static-resource-handler]
   ["/img/*" static-resource-handler]
   ["/js/*" static-resource-handler]])

(def router (rr/router routes))

(def handler
  (rr/ring-handler
   router
   (rr/create-default-handler)))
