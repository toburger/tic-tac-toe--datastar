(ns example.utils
  (:require
   [charred.api :as charred]
   [starfederation.datastar.clojure.api :as d*]))

(def ^:private buf-size 1024)

(def read-json (charred/parse-json-fn {:async? false :bufsize buf-size}))

(defn get-signals [req]
  (-> req d*/get-signals read-json))