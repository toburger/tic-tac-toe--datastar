(ns tic-tac-toe.sse
  (:require
   [starfederation.datastar.clojure.adapter.ring :refer [->sse-response on-open on-close]]
   [starfederation.datastar.clojure.api :as d*]))

(defonce !connections
  (atom #{}))

(defn broadcast! [html]
  (doseq [sse @!connections]
    (try
      (d*/patch-elements! sse html)
      (catch Exception _
        (swap! !connections disj sse)))))

(defn stream-response [request initial-html]
  (let [closed (promise)]
    (->sse-response
     request
     {on-open
      (fn [sse]
        (swap! !connections conj sse)
        (try
          (d*/patch-elements!
           sse
           initial-html)

          ;; The Ring adapter writes synchronously. Keep this callback alive so
          ;; Jetty does not close the response immediately after the first event.
          @closed
          (finally
            (swap! !connections disj sse))))

      on-close
      (fn [sse]
        (swap! !connections disj sse)
        (deliver closed true))})))
