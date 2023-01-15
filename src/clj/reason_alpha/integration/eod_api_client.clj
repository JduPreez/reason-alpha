(ns reason-alpha.integration.eod-api-client
  (:require [ajax.core :refer [GET]]
            [cheshire.core :as json]
            [clojure.string :as str]
            [outpace.config :refer [defconfig]]
            [reason-alpha.utils :as utils]
            [tick.core :as tick]))

;; TODO: Get this from each user's own profile
(defconfig api-token)

(defconfig live-stock-prices-api)

(defn- handle-quote-live-price [*result idx-hid-tkrs response]
  (let [r (map (fn [{:keys [code timestamp open previousClose
                            high low volume change close] :as quote}]
                 (let [ptime (-> timestamp
                                 (tick/new-duration :seconds)
                                 tick/inst)]
                   {:price-id             (utils/new-uuid)
                    :price-creation-id    (utils/new-uuid)
                    :symbol-ticker        code
                    :symbol-provider      :eod-historical-data
                    :holding-id           (get idx-hid-tkrs code)
                    :price-time           ptime
                    :price-open           open
                    :price-close          close
                    :price-high           high
                    :price-low            low
                    :price-previous-close previousClose
                    :price-volume         volume
                    :price-change         change})) response)]
    (deliver *result
             {:result r
              :type   :success})))

(defn- handle-quote-live-price-err
  [*result idx-hid-tkrs {:keys [status status-text]
                         :as   response}]
  (deliver *result
           {:error       response
            :description (str "something bad happened: " status " " status-text)
            :type        :error}))

(defn- quote-live-prices* [api-token' ticker-promises]
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
     :request {:params          (cond-> {:api_token api-token'
                                         :fmt       "json"}
                                  adtnl-syms-str (assoc :s adtnl-syms-str))
               :handler         #(handle-quote-live-price *res idx-hid-tkrs %)
               :error-handler   #(handle-quote-live-price-err *res idx-hid-tkrs %)
               :response-format :json
               :keywords?       true}}))

(defn quote-live-prices
  [api-token' tickers & [{:keys [batch-size]}]]
  (let [batch-size (or batch-size 2)
        parts      (if (< (count tickers) batch-size)
                     [tickers]
                     (vec (partition-all batch-size tickers)))
        tkr-parts  (mapv (fn [tickers] [tickers (promise)]) parts)
        requests   (quote-live-prices* api-token' tkr-parts)
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
        results      (quote-live-prices api-token tickers {:batch-size 3})]
    (doall
     (for [*r results]
       @*r)))

  (let [response     '({:open          166.82,
                        :close         167.76,
                        :volume        212094,
                        :high          169.8,
                        :gmtoffset     0,
                        :code          "ADS.XETRA",
                        :previousClose 168.76,
                        :low           165.7,
                        :change_p      -0.5926,
                        :change        -1,
                        :timestamp     1656681420}
                       {:open          360,
                        :close         354.4,
                        :volume        33385197,
                        :high          363.6,
                        :gmtoffset     0,
                        :code          "0700.HK",
                        :previousClose 365,
                        :low           354,
                        :change_p      -2.9041,
                        :change        -10.6,
                        :timestamp     1656576480}
                       {:open          76.01,
                        :close         76.39,
                        :volume        6504025,
                        :high          76.88,
                        :gmtoffset     0,
                        :code          "SBUX.US",
                        :previousClose 76.43,
                        :low           74.87,
                        :change_p      -0.0523,
                        :change        -0.04,
                        :timestamp     1656619200})
        idx-hid-tkrs {"ADS.XETRA" 1, "0700.HK" 2, "SBUX.US" 3}
        *result      (promise)]
    (handle-quote-live-price *result idx-hid-tkrs response)
    @*result)

  )
