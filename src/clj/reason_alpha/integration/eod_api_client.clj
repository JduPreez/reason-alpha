(ns reason-alpha.integration.eod-api-client
  (:require [ajax.core :refer [GET]]
            [cheshire.core :as json]
            [clojure.string :as str]
            [outpace.config :refer [defconfig]]))

;; TODO: Get this to each user's own profile
;;(defconfig api-token)
(def api-token' "~~~~~~~~~")

;;(defconfig live-stock-prices-api)
(def live-stock-prices-api "https://eodhistoricaldata.com/api/real-time/%s")

(defn- handle-quote-live-price [*result idx-hid-tkrs response]
  (clojure.pprint/pprint {::handle-quote-live-price response})
  (deliver *result
           {:result response
            :type   :success}))

(defn- handle-quote-live-price-err
  [*result idx-hid-tkrs {:keys [status status-text]
                         :as   response}]
  (clojure.pprint/pprint {::handle-quote-live-price-err response})
  (deliver *result
           {:error       response
            :description (str "something bad happened: " status " " status-text)
            :type        :error}))

(defn- quote-live-prices* [api-token ticker-promises]
  (for [[tkrs *res] ticker-promises
        :let        [idx-hid-tkrs   (->> tkrs
                                         (map (fn [[hid t]][t hid]))
                                         (into {}))
                     symbols        (keys idx-hid-tkrs)
                     main-sym       (first symbols)
                     uri            (format live-stock-prices-api main-sym)
                     adtnl-syms     (rest symbols)
                     adtnl-syms-str (when (seq adtnl-syms)
                                      (str/join ","
                                                adtnl-syms))]]
    {:uri     uri
     :request {:params          (cond-> {:api_token api-token
                                         :fmt       "json"}
                                  adtnl-syms-str (assoc :s adtnl-syms-str))
               :handler         #(handle-quote-live-price *res idx-hid-tkrs %)
               :error-handler   #(handle-quote-live-price-err *res idx-hid-tkrs %)
               :response-format :json
               :keywords?       true}}))

(defn quote-live-prices
  [api-token tickers & [{:keys [batch-size]}]]
  (let [batch-size (or batch-size 2)
        parts      (if (< (count tickers) batch-size)
                     [tickers]
                     (vec (partition-all batch-size tickers)))
        tkr-parts  (mapv (fn [tickers] [tickers (promise)]) parts)
        requests   (quote-live-prices* api-token tkr-parts)
        *results   (mapv second tkr-parts)]
    (doall
     (for [{:keys [uri request]} requests]
       (GET uri request)))
    *results))


(comment
  (let [ticker-parts [[[[1 "ADS.XETRA"]
                        [2 "0700.HK"]
                        [3 "SBUX.US"]] (promise)]
                      [[[4 "ULVR.LSE"]
                        [5 "V.US"]
                        [6 "ASML.AS"]] (promise)]
                      [[[7 "KO.US"]
                        [8 "TDOC.US"]
                        [9 "9988.HK"]] (promise)]
                      [[[10 "ABI.BR"]] (promise)]]
        tickers      [[1 "ADS.XETRA"]
                      [2 "0700.HK"]
                      [3 "SBUX.US"]
                      [4 "ULVR.LSE"]
                      [5 "V.US"]
                      [6 "ASML.AS"]
                      [7 "KO.US"]
                      [8 "TDOC.US"]
                      [9 "9988.HK"]
                      [10 "ABI.BR"]]
        results      (quote-live-prices api-token' tickers {:batch-size 3})]
    (doall
     (for [*r results]
       @*r)))

  )
