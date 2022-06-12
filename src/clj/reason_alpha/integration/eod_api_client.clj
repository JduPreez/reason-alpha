(ns reason-alpha.integration.eod-api-client
  (:require [ajax.core :refer [GET]]
            [cheshire.core :as json]
            [clojure.string :as str]
            [outpace.config :refer [defconfig]]))

;; TODO: Get this to each user's own profile
;;(defconfig api-token)
(def api-token "232323-7863-test-token-76127867816")

;;(defconfig live-stock-prices-api)
(def live-stock-prices-api "https://eodhistoricaldata.com/api/real-time/%s")

(defn- handle-quote-live-price [*result response]
  (deliver *result
           {:result response
            :type   :success}))

(defn- handle-quote-live-price-err [*result {:keys [status status-text]
                                             :as   response}]
  (deliver *result
           {:error       response
            :description (str "something bad happened: " status " " status-text)
            :type        :error}))

(defn- quote-live-prices* [ticker-promises]
  (for [[tkrs *res] ticker-promises
        :let        [uri (->> tkrs
                              first
                              (format live-stock-prices-api))
                     adtnl-tkrs (rest tkrs)
                     adtnl-tkrs-str (when (seq adtnl-tkrs)
                                      (str/join ","
                                                adtnl-tkrs))]]
    {:uri     uri
     :request {:params          (cond-> {:api_token api-token
                                         :fmt       "json"}
                                  adtnl-tkrs-str (assoc :s adtnl-tkrs-str))
               :handler         #(handle-quote-live-price *res %)
               :error-handler   #(handle-quote-live-price-err *res %)
               :response-format :json
               :keywords?       true}}))

(defn quote-live-prices
  [_api-token tickers & [{:keys [batch-size job-time-sec]}]]
  (let [batch-size (or batch-size 2)
        parts      (if (< (count tickers) batch-size)
                     [tickers]
                     (vec (partition-all batch-size tickers)))
        tkr-parts  (mapv (fn [p] [p (promise)]) parts)
        *results   (mapv #(second %) tkr-parts)]
    (doall
     (for [{:keys [uri request]} tkr-parts]
       (GET uri request)))
    *results))


(comment
  (let [tiker-parts [[1 "ADS.XETRA"]
                     [2 "0700.HK"]
]])

  )
