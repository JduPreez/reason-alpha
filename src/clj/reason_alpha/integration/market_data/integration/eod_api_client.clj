(ns reason-alpha.integration.market-data.integration.eod-api-client
  (:require [ajax.core :refer [GET]]
            [cheshire.core :as json]
            [clojure.string :as str]
            [outpace.config :refer [defconfig]]
            [reason-alpha.utils :as utils]
            [tick.core :as tick]
            [tick.locale-en-us]))

(defconfig dev-api-token)

(defconfig conf)

(def *real-time-api-uri (delay (:real-time-api-uri conf)))

(def *historic-eod-api-uri (delay (:historic-eod-api-uri conf)))

(defn- handle-historic-prices-err
  [{:keys [*result symbol-ticker date-range]} result]
  (deliver *result {:result-id (utils/new-id)
                    :type      :error
                    :result    {:symbol-ticker symbol-ticker
                                :date-range    date-range}
                    :error     result}))

(defn- handle-historic-prices
  [{:keys [*result symbol-ticker date-range]} result]
  (->> result
       (map
        (fn [{:keys [date open high low close adjusted_close volume]}]
          (let [id (str symbol-ticker "/" date)]
            {:price/id              id
             :price/creation-id     id
             :price/symbol-ticker   symbol-ticker
             :price/symbol-provider :eodhd
             :price/time            date
             :price/type            :historic
             :price/open            open
             :price/close           close
             :price/high            high
             :price/low             low
             :price/volume          volume
             :price/adj-close       adjusted_close})))
       (assoc {:symbol-ticker symbol-ticker
               :date-range    date-range}
              :prices)
       (assoc {:result-id (utils/new-id)
               :type      :success}
              :result)
       (deliver *result)))

(defn- inst-time->date-str
  [t]
  (->> t
       tick/date
       (tick/format date-formatter)))

(defn- request-historic-prices
  [api-token *res symbol-ticker [from to :as dr]]
  (let [uri (format @*historic-eod-api-uri symbol-ticker)]
    (GET uri
         {:params          {:api_token api-token
                            :fmt       "json"
                            :from      (inst-time->date-str from)
                            :to        (inst-time->date-str to)}
          :handler         #(handle-historic-prices {:symbol-ticker symbol-ticker
                                                     :*result       *res
                                                     :date-range    dr} %)
          :error-handler   #(handle-historic-prices-err {:symbol-ticker symbol-ticker
                                                         :*result       *res
                                                         :date-range    dr} %)
          :response-format :json
          :keywords?       true})
    *res))

(def date-formatter (tick/formatter "yyyy-MM-dd"))

(defn quote-historic-prices
  [api-token & {:keys [symbol-ticker date-range]}]
  (let [*result (promise)]
    (request-historic-prices
     api-token
     *result
     symbol-ticker
     date-range)
    *result))

#_(defn- handle-quote-live-price
    [*result idx-hid-tkrs response]
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

#_(defn- handle-quote-live-price-err
  [*result idx-hid-tkrs {:keys [status status-text]
                         :as   response}]
  (deliver *result
           {:error       response
            :description (str "Error occurred quoting live stock prices: " status " " status-text)
            :type        :error}))

(defn- request-latest-intraday-prices
  [api-token *sym-ticker-proms]
  (doseq [[sym-tkrs *r] *sym-ticker-proms
          :let          [idx-hid-tkrs   (->> sym-tkrs
                                             (map (fn [[hid t]][t hid]))
                                             (into {}))
                         symbols        (keys idx-hid-tkrs)
                         main-sym       (first symbols)
                         uri            (format @*real-time-api-uri main-sym)
                         adtnl-syms     (rest symbols)
                         adtnl-syms-str (when (seq adtnl-syms)
                                          (str/join ","
                                                    adtnl-syms))]]
    (GET uri
         {:params          (cond-> {:api_token api-token
                                    :fmt       "json"}
                             adtnl-syms-str (assoc :s adtnl-syms-str))
          :handler         #(handle-quote-live-price *r idx-hid-tkrs %)
          :error-handler   #(handle-quote-live-price-err *r idx-hid-tkrs %)
          :response-format :json
          :keywords?       true})))

(defn quote-latest-intraday-prices
  [api-token & {:keys [symbol-tickers batch-size]}]
  (let [batch-size     (or batch-size 2)
        parts          (if (< (count symbol-tickers) batch-size)
                         [symbol-tickers]
                         (vec (partition-all batch-size symbol-tickers)))
        tkr-parts      (mapv (fn [symbol-tickers] [symbol-tickers (promise)]) parts)
        *results       (mapv second tkr-parts)
        *many->1result (promise)]
    (request-latest-intraday-prices api-token tkr-parts)
    (->> *results
         (pmap #(deref %))
         concat)
    *many->1result))


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
