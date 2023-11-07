(ns reason-alpha.services.holding-service-test
  (:require [clojure.test :refer :all]
            [reason-alpha.integration.fake-eod-api-client :as eod]
            [reason-alpha.services.holding-service :as sut]))

;; TODO: Fix this test. Need to create mock market data functions
#_(deftest test-assoc-close-prices-fn
  (let [fun               (#'sut/assoc-market-data-fn (fn [aid]
                                                        {:account/subscriptions
                                                         {:subscription/eod-historical-data
                                                          {:api-token "djhjdhd"}}})
                                                      [])
        p                 [{:holding
                            [#uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5"
                             "Sony"],
                            :open-price 33,
                            :open-time
                            #inst "2022-05-18T00:00:00.000-00:00",
                            :stop       20,
                            :position-creation-id
                            #uuid "ab5e3c79-3334-4e52-86e5-e1eec94eaccc",
                            :status     :open,
                            :position-id
                            #uuid "01811a3f-638f-d0de-4ddd-ec224bc81cf1",
                            :holding-position-id
                            #uuid "0180098b-e65e-d7ce-645b-41eef737fa0c",
                            :holding-id
                            #uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5",
                            :quantity   23,
                            :eod-historical-data
                            "6758.TSE",
                            :long-short [:long ""]}
                           {:trade-pattern
                            [#uuid "01800865-9069-63c7-9c6c-4d24cdcefc9a"
                             "Breakout"],
                            :holding
                            [#uuid "018004bb-227c-d6de-69ac-bc1eab688ab5"
                             "Walt Disney"],
                            :open-price  23,
                            :open-time
                            #inst "2022-04-06T00:00:00.000-00:00",
                            :stop        56,
                            :position-creation-id
                            #uuid "74e921b2-fe79-454d-b47c-08c43b298019",
                            :status      :closed,
                            :close-price 95.86,
                            :position-id
                            #uuid "0180098b-e65e-d7ce-645b-41eef737fa0c",
                            :holding-id
                            #uuid "018004bb-227c-d6de-69ac-bc1eab688ab5",
                            :quantity    676,
                            :eod-historical-data
                            "DIS.US",
                            :long-short  [:long ""]}
                           {:holding
                            [#uuid "01809f38-c167-6811-e9ef-c2edd166236d"
                             "Unity"],
                            :open-price          34,
                            :open-time
                            #inst "2022-05-05T00:00:00.000-00:00",
                            :stop                34,
                            :position-creation-id
                            #uuid "9f91f95c-43c3-46e6-a661-5409959a42b2",
                            :status              :closed,
                            :close-price         46.72,
                            :position-id
                            #uuid "0180ae8d-abff-be83-dcde-a31bfe42ab41",
                            :holding-position-id
                            #uuid "0180098b-e65e-d7ce-645b-41eef737fa0c",
                            :holding-id
                            #uuid "01809f38-c167-6811-e9ef-c2edd166236d",
                            :quantity            33,
                            :eod-historical-data "U.US",
                            :long-short          [:long ""]}
                           {:trade-pattern
                            [#uuid "0180088d-aa18-6709-de16-4d2e56126947"
                             "zzzzzz"],
                            :holding
                            [#uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5"
                             "Sony"],
                            :open-price  23,
                            :open-time
                            #inst "2022-04-20T00:00:00.000-00:00",
                            :stop        2342,
                            :position-creation-id
                            #uuid "0d4a7fdf-5ab0-4d08-a35a-c5b23fa46c6e",
                            :status      :closed,
                            :close-price 8.07,
                            :position-id
                            #uuid "018008e1-a0db-2638-b2e2-4e9f0e332d11",
                            :holding-position-id
                            #uuid "0180098b-e65e-d7ce-645b-41eef737fa0c",
                            :holding-id
                            #uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5",
                            :quantity    50,
                            :eod-historical-data
                            "6758.TSE",
                            :long-short  [:long ""]}]
        p-with-cls-prices (fun "test" p)]
    (is (= (count p-with-cls-prices) 4))
    (is (every? :close-price p-with-cls-prices))))

(comment
  (clojure.test/run-tests 'reason-alpha.services.holding-service-test)
  )
