(ns reason-alpha.integration.fake-eod-api-client
  (:require [reason-alpha.utils :as utils]
            [tick.core :as tick]))

(defn quote-live-prices
  [api-token tickers & [{:keys [batch-size]
                         :or   {batch-size 15}}]]
  (let [parts     (if (< (count tickers) batch-size)
                    [tickers]
                    (partition batch-size tickers))
        tkr-parts (map (fn [p] [p (promise)]) parts)
        results   (map #(second %) tkr-parts)]
    (doall
     (for [[tkrs *reslt] tkr-parts]
       (future
         (Thread/sleep 2000) ;; Wait 2 seconds
         (let [prices (for [[hid t] tkrs]
                        {:price-id             (utils/new-uuid)
                         :price-creation-id    (utils/new-uuid)
                         :symbol-ticker        t
                         :symbol-provider      :eod-historical-data
                         :holding-id           hid
                         :price-time           (tick/now)
                         :price-open           (rand 100)
                         :price-close          (rand 100)
                         :price-high           (rand 100)
                         :price-low            (rand 100)
                         :price-previous-close (rand 100)
                         :price-volume         (rand-int 1000000)
                         :price-change         (if (even? (rand-int 100))
                                                 (rand 100)
                                                 (* -1 (rand 100)))})]
           (deliver *reslt prices)))))
    results))



(comment
  (let [tickers [[1 "ADS.XETRA"]
                 [2 "0700.HK"]
                 [3 "NXFIL.AS"]
                 [4 "TDOC.US"]
                 [5 "FB.US"]
                 [6 "RTX.US"]
                 [7 "ARKK.US"]
                 [8 "ARKG.US"]
                 [9 "ARKF.US"]
                 [10 "KO.US"]]
        results (quote-live-prices nil tickers {:partition-size 2})]
    (doall
     (for [*r results]
       @*r)))


  )
