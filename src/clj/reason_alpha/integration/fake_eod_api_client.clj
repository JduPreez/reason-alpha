(ns reason-alpha.integration.fake-eod-api-client
  (:require [reason-alpha.utils :as utils]
            [tick.core :as tick]))

(defn quote-live-prices
  [_api-token tickers & [{:keys [batch-size job-time-sec]}]]
  (let [batch-size   (or batch-size 2)
        job-time-sec (or job-time-sec 2)
        parts        (if (< (count tickers) batch-size)
                       [tickers]
                       (vec (partition-all batch-size tickers)))
        tkr-parts    (mapv (fn [p] [p (promise)]) parts)
        results      (mapv #(second %) tkr-parts)]
    (doall
     (for [[tkrs *reslt] tkr-parts]
       (future
         (Thread/sleep (* job-time-sec 1000)) ;; Wait X seconds
         (let [prices (for [[hid t] tkrs
                            :let    [price-change (if (even? (rand-int 100))
                                                    (rand 100)
                                                    (* -1 (rand 100)))]]
                        {:price-id             (utils/new-uuid)
                         :price-creation-id    (utils/new-uuid)
                         :symbol-ticker        t
                         :symbol-provider      :marketstack
                         :holding-id           hid
                         :price-time           (tick/now)
                         :price-open           (double (rand 100))
                         :price-close          (double (rand 100))
                         :price-high           (double (rand 100))
                         :price-low            (double (rand 100))
                         :price-previous-close (double (rand 100))
                         :price-volume         (rand-int 1000000)
                         :price-change         (double price-change)})]
           (deliver *reslt prices)))))
    results))



(comment
  (let [tickers [[1 "ADS.XETRA"]
                 [2 "0700.HK"]
                 [3 "NXFIL.AS"]
                 ;; [4 "TDOC.US"]
                 ;; [5 "FB.US"]
                 ;; [6 "RTX.US"]
                 ;; [7 "ARKK.US"]
                 ;; [8 "ARKG.US"]
                 ;; [9 "ARKF.US"]
                 ;; [10 "KO.US"]
                 ]
        results (quote-live-prices nil tickers)]
    (partition-all 2 tickers)
    #_(doall
       (for [*r results]
       @*r)))

  (let [f          (rand 100)
        dec-places 2]
    )

  )
