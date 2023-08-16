(ns reason-alpha.integration.exchangerate-host-api-client
  (:require [ajax.core :as ajax :refer [GET]]
            [clojure.core [memoize :as memo]]
            [clojure.core.async :as as]
            [clojure.instant :as instant]
            [clojure.string :as str]
            [malli.core :as m]
            [reason-alpha.model.fin-instruments :as model]
            [reason-alpha.utils :as utils]
            [tick.core :as tick])
  (:import [java.util Date]))

(def ^:const base-uri "https://api.exchangerate.host/")

(def latest-uri (str base-uri "latest"))

(def convert-uri (str base-uri "convert"))

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

;; TODO: Rewrite to use channels
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

(def *cache (utils/ttl-cache))

(defn- cache-key
  [{:keys [from to date]}]
  (str (str date ) "-" from "-" to))

(defn- convert-success
  [result-out-chnl conversion {:keys [result]}]
  (let [c (assoc conversion :fx-rate result)]
    (as/>!! result-out-chnl c)
    (utils/set-cache-item *cache (cache-key c) c)))

(defn- convert-error
  [result-out-chnl conversion {:keys [status status-text] :as r}]
  (as/>!! result-out-chnl (assoc conversion :error r)))

(defn- request-convert
  [result-out-chnl {:keys [from to] :as conversion}]
  (as/go
    (let [f   (if (keyword? from) (name from) from)
          t   (if (keyword? to) (name to) to)
          req {:params {:from            f
                        :to              t
                        :format          :json
                        :response-format :json
                        :handler         (partial convert-success
                                                  result-out-chnl conversion)
                        :error-handler   (partial convert-error
                                                  result-out-chnl conversion)
                        :keywords?       true}}]
      @(GET (str base-uri convert-uri)
            req))))

(defn- convert-cached
  [result-out-chnl currency-conversions]
  (filterv (fn [{:keys [from to] :as convr}]
             (let [c (utils/get-cache-item *cache (cache-key convr))]
               (if (= ::utils/nil-cache-item c)
                 convr
                 #_else (let [_ (as/>!! result-out-chnl c)]))))
           currency-conversions))

(defn convert
  [currency-conversions]
  (let [buffer-size     (count currency-conversions)
        result-out-chnl (as/chan buffer-size)
        convrs          (->> currency-conversions
                             (map (fn [{:keys [date] :as c}]
                                    (if date
                                      c
                                      (assoc c :date (tick/now)))))
                             (convert-cached result-out-chnl))]
    (as/go
      (doseq [c convrs]
        (request-convert result-out-chnl c)))
    (as/take buffer-size result-out-chnl)))

(comment
  ([from-currency to-currency date]
   (let [dte-str (-> date
                     (tick/in "UTC")
                     tick/date
                     str)]))


  (-> (tick/now)
      (tick/in "UTC")
      (tick/date))


  @(latest :USD :ZAR)

  @(latest :USD :SGD)

  @(latest :EUR :ZAR)

  )
