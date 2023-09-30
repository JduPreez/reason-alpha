(ns reason-alpha.integration.exchangerate-host-api-client
  (:require [ajax.core :as ajax :refer [GET]]
            [clojure.core [memoize :as memo]]
            [clojure.core.async :as as]
            [clojure.instant :as instant]
            [clojure.string :as str]
            [malli.core :as m]
            [outpace.config :refer [defconfig]]
            [reason-alpha.model.fin-instruments :as model]
            [reason-alpha.utils :as utils]
            [tick.core :as tick])
  (:import [java.util Date]))

(defconfig access-key)

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
    (GET latest-uri {:params          {:base       base
                                       :symbols    syms
                                       :places     6
                                       :access_key access-key}
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
  (str date "-" from "-" to))

(defn- convert-success
  [result-out-chnl conversion {:keys [result] :as x}]
  (let [c (assoc conversion :fx-rate result)]
    (clojure.pprint/pprint {:-_### x})
    (as/>!! result-out-chnl c)
    (utils/set-cache-item *cache (cache-key c) c)))

(defn- convert-error
  [result-out-chnl conversion r]
  (as/>!! result-out-chnl (assoc conversion :error r)))

(defn- request-convert
  [result-out-chnl {:keys [date from to] :as conversion}]
  (as/go
    (let [f   (if (keyword? from) (name from) from)
          t   (if (keyword? to) (name to) to)
          req {:params          {:from       f
                                 :to         t
                                 :date       (str date)
                                 :amount     1
                                 :access_key access-key}
               :format          :json
               :response-format :json
               :handler         (partial convert-success
                                         result-out-chnl conversion)
               :error-handler   (partial convert-error
                                         result-out-chnl conversion)
               :keywords?       true}]
      @(GET convert-uri req))))

(defn- convert-cached
  [result-out-chnl currency-conversions]
  (->> currency-conversions
       (filterv (fn [{:keys [from to] :as convr}]
                  (let [c (utils/get-cache-item *cache (cache-key convr))]
                    (if (= ::utils/nil-cache-item c)
                      convr
                      #_else (utils/ignore
                              (as/>!! result-out-chnl c))))))
       (remove nil?)))

(defn convert
  [currency-conversions]
  (let [buffer-size     (count currency-conversions)
        result-out-chnl (as/chan buffer-size)]
    (as/go
      (let [convrs (->> currency-conversions
                        (map (fn [{:keys [date] :as c}]
                               (->> (tick/inst)
                                    (or date)
                                    tick/date
                                    #_(tick/at "00:00")
                                    #_tick/inst
                                    (assoc c :date))))
                        (convert-cached result-out-chnl))]
        (doseq [c convrs]
          (request-convert result-out-chnl c))))
    (as/take buffer-size result-out-chnl)))

(comment
  (let [c (convert '({:from :EUR,
                      :to   :ZAR,
                      :date nil}) #_[{:from :USD :to :ZAR}
                                     {:from :EUR :to :ZAR}
                                     {:from :GEL :to :ZAR}])]
    (println (as/<!! c))
    #_(println (as/<!! c))
    #_(println (as/<!! c)))


  (-> (tick/now)
      ;;(tick/in "UTC")
      (tick/date)
      #_(tick/at "00:00")
      #_(tick/inst))



  )
