(ns reason-alpha.model.common-test
  (:require [reason-alpha.model.common :as sut]
            [clojure.test :refer :all]))

(deftest test-compute-dependent-fields
  (let [data          [{:id         1
                        :stop-loss  -760
                        :quantity   152
                        :open-price 71.83}
                       {:id         2
                        :stop-loss  -7878
                        :quantity   344
                        :open-price 562}
                       {:id         3
                        :stop-loss  -901
                        :quantity   23
                        :open-price 184.8}
                       {:id         4
                        :stop-loss  -215
                        :quantity   512
                        :open-price 87.11}]
        comps         {:stop-percent-loss
                       {:function
                        "PERCENT(stop-loss/(quantity * open-price))"}
                       :xyz
                       {:require [:stop-percent-loss]
                        :function
                        "100 - IF(stop-percent-loss < 0, stop-percent-loss * -1, stop-percent-loss)"}
                       :multiply-by-2
                       {:require [:stop-percent-loss :xyz]
                        :function
                        "ROUND((xyz + stop-percent-loss) * 2, 2)"}}
        computed-data (-> data (sut/compute {:computations comps}) :result)]
    (doseq [{:keys [id xyz multiply-by-2]} computed-data]
      (case id
        1 (do (is (= xyz 93.04))
              (is (= multiply-by-2 172.16)))
        2 (do (is (= xyz 95.93))
              (is (= multiply-by-2 183.72)))
        3 (do
            (is (= xyz 78.8))
            (is (= multiply-by-2 115.2)))
        4 (do
            (is (= xyz 99.52))
            (is (= multiply-by-2 198.08)))))))

(deftest test-computations
  (let [model                  [:map
                                [:position-creation-id {:command-path [:position/creation-id]}
                                 uuid?]
                                [:position-id {:optional     true
                                               :command-path [:position/id]} uuid?]
                                [:holding {:title        "Holding (Instrument)"
                                           :ref          :holding
                                           :command-path [[:position/holding-id]
                                                          [:holding/instrument-name]]}
                                 [:tuple uuid? string?]]
                                [:long-short {:title        "Long/Short (Hedge)"
                                              :ref          :position/long-short
                                              :command-path [[:position/long-short]
                                                             [:position/long-short-name]]}
                                 [:tuple keyword? string?]]
                                [:open-time {:title        "Open Time"
                                             :command-path [:position/open
                                                            :trade-transaction/date]}
                                 inst?]
                                [:quantity {:title        "Quantity"
                                            :command-path [:position/open
                                                           :trade-transaction/quantity]}
                                 number?]
                                [:open-price {:title        "Open"
                                              :command-path [:position/open
                                                             :trade-transaction/price]}
                                 number?]
                                [:open-total {:title    "Open Total"
                                              :optional true
                                              :compute  {:function "quantity * open-price"
                                                         :use      [:quantity :open-price]}}
                                 number?]
                                [:close-price {:title        "Close"
                                               :optional     true
                                               :command-path [:position/close
                                                              :trade-transaction/price]}
                                 number?]
                                [:status {:optional     true
                                          :command-path [:position/status]}
                                 keyword?]
                                [:stop {:optional     true
                                        :title        "Stop"
                                        :command-path [:position/stop]}
                                 number?]
                                [:trade-pattern {:title        "Trade Pattern"
                                                 :optional     true
                                                 :ref          :trade-pattern
                                                 :command-path [[:position/trade-pattern-id]
                                                                [:trade-pattern/name]]}
                                 [:tuple uuid? string?]]
                                [:holding-position-id {:title        "Holding Position"
                                                       :optional     true
                                                       :ref          :position/holding-position
                                                       :command-path [:position/holding-position-id]}
                                 uuid?]
                                [:stop-loss {:title    "Stop Loss"
                                             :optional true}
                                 float?]
                                [:eod-historical-data {:optional     true
                                                       :fn-value     {:arg :symbol/provider
                                                                      :fun '(fn [{p :symbol/provider
                                                                                  v :symbol/ticker}]
                                                                              (when (= p :eod-historical-data)
                                                                                {:value v}))}
                                                       :command-path [:holding/symbols 0 :symbol/ticker]}
                                 string?]
                                [:holding-id {:optional     true
                                              :command-path [:position/holding-id]}
                                 uuid?]
                                [:stop-loss-percent {:optional true
                                                     :title    "Stop Loss % of Allocation"
                                                     :compute  {:function "TPERCENT(stop-loss/(quantity * open-price))"
                                                                :use      [:stop-loss :quantity :open-price]}} float?]]
        {{slp-fn :function} :stop-loss-percent
         {ot-fn :function}  :open-total
         :as                x} (sut/computations model)]
    (is slp-fn)
    (is ot-fn)))

(comment
  (clojure.test/run-tests 'reason-alpha.model.common-test)

)
