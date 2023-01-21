(ns reason-alpha.integration.exchangerate-host-api-client
  (:require [ajax.core :as ajax :refer [GET]]
            [clojure.core [memoize :as memo]]
            [clojure.instant :as instant]
            [clojure.string :as str]
            [malli.core :as m]
            [reason-alpha.model.fin-instruments :as model]
            [reason-alpha.utils :as utils]
            [tick.core :as tick])
  (:import [java.util Date]))

(def latest-uri "https://api.exchangerate.host/latest")

(defn- latest-success
  [*result {:keys [base date rates] :as response}]
  (let [base-k   (keyword base)
        dte-inst (instant/read-instant-date date)
        r        (->> rates
                      (map
                       (fn map-rates [[currency rate]]
                         [currency {:exchange-rate-creation-id (utils/new-uuid)
                                    :exchange-rate-id          (utils/new-uuid)
                                    :base-currency             base-k
                                    :other-currency            currency
                                    :rate                      rate
                                    :date-time                 dte-inst}]))
                      (into {}))]
    (deliver *result
             {:result r
              :type   :success})))

(defn- latest-error
  [*result response]
  (deliver *result
           {:error       response
            :description "An error occurred fetching fx data"}))

(defn- latest-by-base-currency
  [base-currency]
  (let [base    (if (keyword? base-currency)
                  (name base-currency)
                  base-currency)
        syms    (->> model/Currency
                     m/children
                     (map name)
                     (str/join ","))
        *result (promise)]
    (GET latest-uri {:params          {:base    base
                                       :symbols syms
                                       :places  6}
                     :handler         #(latest-success *result %)
                     :error-handler   #(latest-error *result %)
                     :response-format :json
                     :keywords?       true})
    @*result))

;; Cache for 1 hour
(def latest-by-base-currency-cached (memo/ttl latest-by-base-currency
                                              :ttl/threshold 3600000))

(defn latest
  [base-currency other-currency]
  (let [*result (promise)]
    (future
      (let [{:keys [error result]
             :as   r} (latest-by-base-currency-cached base-currency)]
        (if (not error)
          (deliver *result (get result other-currency))
          (deliver *result r))))
    *result))



(comment
  @(latest :USD :ZAR)

  @(latest :USD :SGD)

  @(latest :EUR :ZAR)

  )
