(ns example.core
  (:require [dev.onionpancakes.chassis.compiler :as hc]
            [dev.onionpancakes.chassis.core :as h]
            [example.utils :as utils]
            [reitit.ring :as rr]
            [reitit.ring.middleware.parameters :as rmparams]
            [ring.util.response :as ruresp]
            [starfederation.datastar.clojure.adapter.ring :refer [->sse-response
                                                                  on-open]]
            [starfederation.datastar.clojure.api :as d*]
            [clojure.string :as string]))

(defn linear-gradient
  [colors & {:keys [direction] :or {direction "right"}}]
  {:pre [(>= (count colors) 2)]}
  (str "linear-gradient("
       "to " direction " in oklch, " (string/join ", " colors)
       ");"))

(comment
  (linear-gradient ["red" "blue"])
  (linear-gradient ["red" "blue"] {:direction "left"})
  (linear-gradient ["red" "blue"] :direction "left"))

(def home-html
  [:html {:lang "en"}
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:title "Datastar SDK Demo"]
    [:script {:src "https://unpkg.com/@tailwindcss/browser@4"}]
    [:script
     {:type "module"
      :src
      "https://cdn.jsdelivr.net/gh/starfederation/datastar@main/bundles/datastar.js"}]]
   [:body.bg-white.text-lg.max-w-xl.mx-auto.my-16 {:class ["dark:bg-gray-900"]}
    [:div.bg-white.text-gray-500.rounded-lg.px-6.py-8.ring.shadow-xl.space-y-2
     {:data-signals:delay "400"
      :class ["dark:bg-gray-800" "dark:text-gray-400" "ring-gray-900/5"]}
     [:div.flex.justify-between.items-center
      [:h1.text-gray-900.text-3xl.font-semibold {:class ["dark:text-white"]}
       "Datastar SDK Demo"]
      [:img {:src "https://data-star.dev/static/images/rocket-64x64.png"
             :alt "Rocket"
             :width "64"
             :height "64"}]]
     [:p.mt-2 "SSE events will be streamed from the backend to the frontend."]
     [:div.space-x-2
      [:label {:for "delay"}
       "Delay in milliseconds"]
      [:input#delay.w-36.rounded-md.border.border-gray-300.px-3.py-2.placeholder-gray-400.shadow-sm
       {:data-bind:delay true
        :type "number"
        :step "100"
        :min "0"
        :class ["focus:border-sky-500"
                "focus:outline"
                "focus:outline-sky-500"
                "dark:disabled:border-gray-700"
                "dark:disabled:bg-gray-800/20"]}]]
     [:button.rounded-md.bg-sky-500.px-5.leading-5.font-semibold.text-white.cursor-pointer
      {:data-on:click "@get('/hello-world')"
       :class ["py-2.5" "hover:bg-sky-700" "hover:text-gray-100"]}
      "Start"]]
    [:div.my-16.text-8xl.font-bold.text-transparent
     {:style
      {:background (linear-gradient ["red" "orange" "yellow" "green" "blue" "blue" "violet"])
       :background-clip :text}}
     [:div#message "Hello, world!"]]]])

(def home-page
  (str h/doctype-html5
       (h/html home-html)))

(defn home [_]
  (-> home-page
      (ruresp/response)
      (ruresp/content-type "text/html")))

(def message "Hello, world!")

(def msg-count (count message))

(defn ->frag [i]
  (h/html
   (hc/compile
    [:div {:id "message"}
     (subs message 0 (inc i))])))

(defn hello-world [request]
  (let [d (-> request utils/get-signals (get "delay") int)]
    (->sse-response request
                    {on-open
                     (fn [sse]
                       (d*/with-open-sse sse
                         (dotimes [i msg-count]
                           (d*/patch-elements! sse (->frag i))
                           (Thread/sleep d))))})))

(def routes
  [["/" {:handler home}]
   ["/hello-world" {:handler hello-world
                    :middleware [rmparams/parameters-middleware]}]])

(def router (rr/router routes))

(def handler (rr/ring-handler router))