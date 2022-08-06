(ns reason-alpha.utils-test
  (:require  [clojure.test :refer :all]
             [reason-alpha.utils :as sut]
             [reason-alpha.integration.fake-eod-api-client :as eod]))

;; (deftest do-if-realized
;;   (let [tickers        [[1 "ADS.XETRA"]
;;                         [2 "0700.HK"]
;;                         [3 "NXFIL.AS"]
;;                         [4 "TDOC.US"]
;;                         [5 "RTX.US"]
;;                         [6 "ARKK.US"]]
;;         *do-call-count (atom 0)
;;         batch-size     2
;;         results        (eod/quote-live-prices nil tickers {:batch-size   batch-size
;;                                                            :job-time-sec 1})]
;;     (sut/do-if-realized results
;;                         (fn [_] (swap! *do-call-count inc)))
;;     (is (= (/ (count tickers) batch-size)
;;            @*do-call-count))))

(comment
  (clojure.test/run-tests 'reason-alpha.utils-test)

  )

