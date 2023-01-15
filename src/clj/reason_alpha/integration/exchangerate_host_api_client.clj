(ns reason-alpha.integration.exchangerate-host-api-client
  (:require [ajax.core :as ajax :refer [GET]]
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
  ;; Create 2 ExchangeRateDto items
  ;; USDZAR: 1 USD = 17.105669 ZAR
  ;; ZARUSD: 1 ZAR = 1/17.105669 USD
  (let [base-k   (keyword base)
        dte-inst (instant/read-instant-date date)
        r        (->> rates
                      (mapcat
                       (fn map-rates [[currency rate]]
                         [{:exchange-rate-creation-id (utils/new-uuid)
                           :exchange-rate-id          (utils/new-uuid)
                           :base-currency             base-k
                           :other-currency            currency
                           :rate                      rate
                           :date-time                 dte-inst}
                          {:exchange-rate-creation-id (utils/new-uuid)
                           :exchange-rate-id          (utils/new-uuid)
                           :base-currency             other-currency
                           :other-currency            base-k
                           :rate                      (/ 1 rate)
                           :date-time                 dte-inst}])))]
    (deliver *result
             {:result r
              :type   :success})))

;; TODO: Change this into a promise
;; TODO: Chache results + try to get fx-rate from cache 1st
(defn- latest*
  [base-currency other-currency]
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
                     ;; TODO: Deliver result to promise
                     :handler         #(clojure.pprint/pprint {::exchange-latest-success %})
                     :error-handler   #(clojure.pprint/pprint {::exchange-latest-failure %})
                     :response-format :json
                     :keywords?       true})
    *result))

(comment
  (latest {:base-currency :ZAR
           :symbols       [:USD]})

  (latest {:base-currency :USD
           :symbols       [:ZAR]})


  )
