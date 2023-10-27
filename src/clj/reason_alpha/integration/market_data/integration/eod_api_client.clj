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

(def date-formatter (tick/formatter "yyyy-MM-dd"))

(defn- handle-historic-prices-err
  [{:keys [*result symbol-ticker date-range]} result]
  (deliver *result {:result-id (utils/new-id)
                    :type      :error
                    :error     {:symbol-ticker symbol-ticker
                                :date-range    date-range
                                :error         result}}))

(defn- handle-historic-prices
  [{:keys [*result symbol-ticker date-range]} result]
  (->> result
       (map
        (fn [{:keys [date open high low close adjusted_close volume]}]
          (let [id (str "historic/" symbol-ticker "/" date)]
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

(defn quote-historic-prices
  [api-token & {st               :symbol-ticker
                [from to :as dr] :date-range}]
  (let [*res (promise)
        uri  (format @*historic-eod-api-uri st)]
    (GET uri
         {:params          {:api_token api-token
                            :fmt       "json"
                            :from      (inst-time->date-str from)
                            :to        (inst-time->date-str to)}
          :handler         #(handle-historic-prices {:symbol-ticker st
                                                     :*result       *res
                                                     :date-range    dr} %)
          :error-handler   #(handle-historic-prices-err {:symbol-ticker st
                                                         :*result       *res
                                                         :date-range    dr} %)
          :response-format :json
          :keywords?       true})
    *res))

(defn- quote-last-historic-prices
  [api-token symbol-tickers]
  (let [n    (tick/now)
        to   (utils/time-at-beginning-of-day n)
        from (->> :weeks
                  (tick/new-duration 1)
                  (tick/<< n)
                  utils/time-at-beginning-of-day)
        dr   [from to]]
    (->> symbol-tickers
         (pmap (fn [sym-tkr]
                 (let [res @(quote-historic-prices api-token
                                                   :symbol-ticker sym-tkr
                                                   :date-range dr)]
                   res))))))

(defn- handle-latest-intraday-prices
  [api-token *result response]
  (let [response           (if (sequential? response)
                             response
                             #_else [response])
        {na     :na
         intrad :intraday} (reduce (fn [r {:keys [code timestamp open previousClose
                                                  high low volume change close]
                                           :as   quote}]
                                     (if (number? close)
                                       (let [ptime (-> timestamp
                                                       (tick/new-duration :seconds)
                                                       tick/inst)
                                             id    (str "intraday/" code "/" ptime)
                                             p     {:price/id              id
                                                    :price/creation-id     id
                                                    :price/symbol-ticker   code
                                                    :price/symbol-provider :eodhd
                                                    :price/time            ptime
                                                    :price/type            :intraday
                                                    :price/open            open
                                                    :price/close           close
                                                    :price/high            high
                                                    :price/low             low
                                                    :price/volume          volume
                                                    :price/previous-close  previousClose
                                                    :price/change          change}]
                                         (update r :intraday #(conj (or % #{}) {:type   :success
                                                                                :result p})))
                                       #_else
                                       (update r :na #(conj (or % []) code))))
                                   {:intraday-prices nil
                                    :na              nil}
                                   response)
        r                  (if (seq na)
                             (concat intrad (quote-last-historic-prices api-token na))
                             intrad)]
    (deliver *result r)))

(defn- handle-latest-intraday-err
  [*result sym-tkrs {:keys [status status-text]
                     :as   response}]
  ;; TODO: Map each sym-tkr to an `:error`
  (deliver *result
           {:error       {:symbol-tickers sym-tkrs
                          :response       response}
            :description (str "Error occurred quoting live stock prices: " status " " status-text)
            :type        :error}))

(defn- request-latest-intraday-prices
  [api-token sym-ticker-batches]
  (doseq [[sym-tkrs *r] sym-ticker-batches
          :let          [main-sym-tkr   (first sym-tkrs)
                         uri            (format @*real-time-api-uri main-sym-tkr)
                         adtnl-syms     (rest sym-tkrs)
                         adtnl-syms-str (when (seq adtnl-syms)
                                          (str/join ","
                                                    adtnl-syms))]]
    (GET uri
         {:params          (cond-> {:api_token api-token
                                    :fmt       "json"}
                             adtnl-syms-str (assoc :s adtnl-syms-str))
          :handler         #(handle-latest-intraday-prices api-token *r %)
          :error-handler   #(handle-latest-intraday-err *r sym-tkrs %)
          :response-format :json
          :keywords?       true})))

(defn quote-latest-intraday-prices
  [api-token & {:keys [symbol-tickers batch-size]}]
  (let [*many->1result (promise)]
    (future
      (let [batch-size  (or batch-size 2)
            batches     (if (< (count symbol-tickers) batch-size)
                          [symbol-tickers]
                          (vec (partition-all batch-size symbol-tickers)))
            tkr-batches (mapv (fn [symbol-tickers] [symbol-tickers (promise)]) batches)
            *results    (mapv second tkr-batches)]
        (request-latest-intraday-prices api-token tkr-batches)
        (let [prices (->> *results
                          (pmap #(deref %))
                          (apply concat))
              rtype  (cond
                       (every? #(= (:type %) :success)
                               prices)
                       , :success
                       (every? #(= (:type %) :error)
                               prices)
                       , :error

                       :else :some-success-err)]
          (deliver *many->1result {:type     rtype
                                   :result   prices
                                   :nr-items (count prices)}))))
    *many->1result))

(comment

  (->> [[1 2 3] [4 5 6] [7 8 9]]
       (apply concat))

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
