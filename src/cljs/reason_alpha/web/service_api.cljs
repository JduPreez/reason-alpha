(ns reason-alpha.web.service-api
  (:require [ajax.core :refer [GET]]
            [clojure.string :as str]
            [cognitect.transit :as transit]
            [luminus-transit.time :as time]))

(def base-uri "http://localhost:3000/api")

(defn- resource [uri]
  (str/join "/" [base-uri uri]))

(defn- default-handler [response]
  (js/console.log (str response)))

(defn- error-handler [{:keys [status status-text]}]
  (js/console.log (str "something bad happened: " status " " status-text)))

(defn- as-transit [opts]
  (merge {:raw             false
          :format          :transit
          :response-format :transit
          :reader          (transit/reader :json time/time-deserialization-handlers)
          :writer          (transit/writer :json time/time-serialization-handlers)}
         opts))

(defn trade-patterns [& [handler]]
  (let [url (resource "trade-patterns")]
    (GET url (as-transit {:handler       (if handler handler default-handler)
                          :error-handler error-handler}))))