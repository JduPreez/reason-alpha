(ns reason-alpha.services.holding-service-test
  (:require [clojure.test :refer :all]
            [reason-alpha.integration.fake-eod-api-client :as eod]
            [reason-alpha.services.holding-service :as sut]))

(def positions [{:close-price                     763.7,
                 :eodhd                           "EL",
                 :holding                         [#uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197" "Adyen"],
                 :holding-currency                :USD,
                 :holding-id                      #uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197",
                 :holding-position-id             #uuid "018a2caa-ba6e-c9a5-8d51-38553003af1f",
                 :long-short                      [:long ""],
                 :open-date                       #inst "2023-10-06T00:00:00.000-00:00",
                 :open-price                      nil,
                 :open-total                      31333.43,
                 :open-total-acc-currency         nil,
                 :position-creation-id            #uuid "d561b5c7-57ab-49b0-84ce-2d04b78f588c",
                 :position-id                     #uuid "018a2cac-c474-3ec7-962c-b5b285877385",
                 :profit-loss-amount              -21.729999999999563,
                 :profit-loss-amount-acc-currency nil,
                 :profit-loss-percent             "-0.07%",
                 :quantity                        41,
                 :status                          :open,
                 :stop                            0,
                 :stop-loss                       -31333.43,
                 :stop-loss-acc-currency          nil,
                 :stop-loss-percent               "-100.0%",
                 :target-profit                   nil,
                 :target-profit-acc-currency      nil,
                 :target-profit-percent           nil,
                 :trade-pattern                   [#uuid "018a2c43-7a96-11a5-ab21-06f37976bbf8" "Breakout"]}
                {:close-price                     nil,
                 :eodhd                           "SOL.JSE",
                 :holding                         [#uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197" "Sasol"],
                 :holding-currency                :EUR,
                 :holding-id                      #uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197",
                 :holding-position-id             #uuid "018a2caa-ba6e-c9a5-8d51-38553003af1f",
                 :long-short                      [:long ""],
                 :open-date                       #inst "2023-03-24T00:00:00.000-00:00",
                 :open-price                      nil,
                 :open-total                      31333.43,
                 :open-total-acc-currency         nil,
                 :position-creation-id            #uuid "cf9da077-0d0c-40d6-b570-f3edd056ca79",
                 :position-id                     #uuid "4b463c08-c0ce-4178-b5b8-1ebce5b7e53a",
                 :profit-loss-amount              -21.729999999999563,
                 :profit-loss-amount-acc-currency nil,
                 :profit-loss-percent             "-0.07%",
                 :quantity                        5,
                 :status                          :open,
                 :stop                            0,
                 :stop-loss                       -31333.43,
                 :stop-loss-acc-currency          nil,
                 :stop-loss-percent               "-100.0%",
                 :target-profit                   nil,
                 :target-profit-acc-currency      nil,
                 :target-profit-percent           nil,
                 :trade-pattern                   [#uuid "018a2c43-7a96-11a5-ab21-06f37976bbf8" "Breakout"]}
                {:close-date                      nil,
                 :close-price                     773.4,
                 :eodhd                           "ADYEN.AS",
                 :holding                         [#uuid "018a465e-82bd-de65-2623-02bb30e1a1f6" "Multiple"],
                 :holding-currency                :SGD,
                 :holding-id                      #uuid "018a465e-82bd-de65-2623-02bb30e1a1f6",
                 :long-short                      [:hedged ""],
                 :open-date                       (utils/time-at-beginning-of-day (tick/inst)),
                 :open-price                      nil,
                 :open-total                      31333.42919921875,
                 :open-total-acc-currency         nil,
                 :position-creation-id            #uuid "3cee7b50-68a9-4fa3-b9eb-5464068ad465",
                 :position-id                     #uuid "018a2caa-ba6e-c9a5-8d51-38553003af1f",
                 :profit-loss-amount              375.9708007812478,
                 :profit-loss-amount-acc-currency nil,
                 :profit-loss-percent             "1.2%",
                 :quantity                        41,
                 :status                          :open,
                 :stop                            0.0,
                 :stop-loss                       -31333.42919921875,
                 :stop-loss-acc-currency          nil,
                 :stop-loss-percent               "-100.0%",
                 :sub-positions                   '(#uuid "d561b5c7-57ab-49b0-84ce-2d04b78f588c"),
                 :target-profit                   nil,
                 :target-profit-acc-currency      nil,
                 :target-profit-percent           nil}])

;; TODO: Implement this 
#_(deftest ^:integration test-assoc-close-prices-fn
  (let [db                (TempMemDb. (atom nil))
        fun               (#'sut/assoc-market-data-fn (fn [aid]
                                                        {:account/subscriptions
                                                         {:subscription/eod-historical-data
                                                          {:api-token "djhjdhd"}}})
                                                      [])
        p-with-cls-prices (fun "test" p)]
    (is (= (count p-with-cls-prices) 4))
    (is (every? :close-price p-with-cls-prices))))

(comment
  (clojure.test/run-tests 'reason-alpha.services.holding-service-test)
  )
