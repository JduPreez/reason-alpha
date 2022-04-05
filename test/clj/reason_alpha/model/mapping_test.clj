(ns reason-alpha.model.mapping-test
  (:require  [clojure.test :refer :all]
             [reason-alpha.model.mapping :as sut]
             [reason-alpha.utils :as utils]))

(def currency-acc-path [:instrument/currency-instrument :instrument/account :account/user-name])

(def query-dto-model [:map
                      [:instrument-id
                       {:optional true, :command-path [:instrument/id]}
                       uuid?]
                      [:instrument-creation-id
                       {:command-path [:instrument/creation-id]}
                       uuid?]
                      [:instrument-name
                       {:title        "Instrument",
                        :optional     true,
                        :command-path [:instrument/name]}
                       string?]
                      [:instrument-type
                       {:title        "Type",
                        :optional     true,
                        :ref          :instrument/type,
                        :command-path [:instrument/type]}
                       keyword?]
                      [:yahoo-finance
                       {:title        "Yahoo! Finance",
                        :optional     true,
                        :pivot        :symbol/provider,
                        :command-path [:instrument/symbols 0 :symbol/ticker]}
                       string?]
                      [:saxo-dma
                       {:title        "Saxo/DMA",
                        :optional     true,
                        :pivot        :symbol/provider,
                        :command-path [:instrument/symbols 0 :symbol/ticker]}
                       string?]
                      [:currency-account
                       {:title        "Currency Account"
                        :optional     true
                        :command-path currency-acc-path}]])

(deftest mapping-query-dto->command-ent
  (let [{:keys [instrument-id
                instrument-creation-id
                instrument-type
                instrument-name
                saxo-dma
                yahoo-finance
                currency-account]
         :as   query-dto} {:instrument-id          (utils/new-uuid)
                           :instrument-creation-id (utils/new-uuid)
                           :instrument-type        :share
                           :instrument-name        "Tencent"
                           :saxo-dma               "00700:xhkg"
                           :yahoo-finance          "0700.hk"
                           :currency-account       "ark@ark.com"}
        {:keys [instrument/creation-id
                instrument/id
                instrument/type
                instrument/name
                instrument/symbols]
         :as   cmd-ent}   (sut/query-dto->command-ent query-dto-model query-dto)]
    (testing "`query-dto->command-ent` should map non-nested path"
      (is (= id instrument-id))
      (is (= creation-id instrument-creation-id))
      (is (= type instrument-type))
      (is (= name instrument-name)))
    (testing "`query-dto->command-ent` should map nested path without vec"
      (is (= currency-account (get-in cmd-ent currency-acc-path))))
    (testing "`query-dto->command-ent` should map nested path with vec"
      (is (= saxo-dma (some
                       (fn [{:keys [symbol/ticker
                                    symbol/provider]}]
                         (when (= provider :saxo-dma)
                           ticker)) symbols)))
      (is (= yahoo-finance (some
                            (fn [{:keys [symbol/ticker
                                         symbol/provider]}]
                              (when (= provider :yahoo-finance)
                                ticker)) symbols))))))

(deftest mapping-tuple-query-dto<->command-ent
  (let [tptrn-id                             #uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc"
        tptrn-nm                             "Breakout"
        instr-id                             #uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc"
        instr-nm                             "Starbucks"
        qry-dto-model                        [:map
                                              [:position-creation-id {:command-path [:position/creation-id]}
                                               uuid?]
                                              [:position-id {:optional     true
                                                             :command-path [:position/id]} uuid?]
                                              [:instrument {:title        "Instrument"
                                                            :ref          :instrument
                                                            :command-path [[:position/instrument-id]
                                                                           [:instrument/name]]}
                                               [:tuple uuid? string?]]
                                              [:quantity {:title        "Quantity"
                                                          :command-path [:position/open-trade-transaction
                                                                         :trade-transaction/quantity]}
                                               float?]
                                              [:open-time {:title        "Open Time"
                                                           :command-path [:position/open-trade-transaction
                                                                          :trade-transaction/date]}
                                               inst?]
                                              #_[:symbols {:optional true} string?]
                                              [:open-price {:title        "Open"
                                                            :command-path [:position/open-trade-transaction
                                                                           :trade-transaction/price]}
                                               float?]
                                              [:close-price {:title        "Close"
                                                             :optional     true
                                                             :command-path [:position/close-trade-transaction
                                                                            :trade-transaction/price]}
                                               float?]
                                              [:trade-pattern {:title        "Trade Pattern"
                                                               :optional     true
                                                               :ref          :trade-pattern
                                                               :command-path [[:position/trade-pattern-id]
                                                                              [:trade-pattern/name]]}
                                               [:tuple uuid? string?]]]
        qry-dto                              {:position-id          #uuid "017fe4f2-b562-236b-f34e-88e227dcf280"
                                              :instrument           [instr-id instr-nm],
                                              :quantity             "778",
                                              :open-time            #inst "2022-04-02T00:00:00.000-00:00",
                                              :open-price           "89789",
                                              :close-price          "89789",
                                              :position-creation-id #uuid "5851072d-4014-48a1-8b5d-507d10a6239b"
                                              :trade-pattern        [tptrn-id tptrn-nm]}
        cmd-ents                             [#:position{:creation-id             #uuid "5851072d-4014-48a1-8b5d-507d10a6239b",
                                                         :id                      #uuid "017fe4f2-b562-236b-f34e-88e227dcf280",
                                                         :instrument-id           #uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc",
                                                         :open-trade-transaction
                                                         #:trade-transaction{:quantity "778",
                                                                             :date     #inst "2022-04-02T00:00:00.000-00:00",
                                                                             :price    "89789"},
                                                         :close-trade-transaction #:trade-transaction{:price "89789"},
                                                         :trade-pattern-id        #uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc"}
                                              #:trade-pattern{:id   tptrn-id
                                                              :name tptrn-nm}
                                              #:instrument{:id   instr-id
                                                           :name instr-nm}]
        {:keys [position/instrument-id
                position/trade-pattern-id]}  (sut/query-dto->command-ent qry-dto-model qry-dto)
        {[tuple-tid tuple-tlbl] :trade-pattern
         [tuple-iid tuple-ilbl] :instrument} (sut/command-ent->query-dto qry-dto-model cmd-ents)]
    (testing "`query-dto->command-ent` should map DTO tuples to entity reference ids"
      (is (= instrument-id instr-id))
      (is (= trade-pattern-id tptrn-id)))
    (testing "`command-ent->query-dto` should map entity aggregates to DTO tuples"
      (is (= tuple-tid tptrn-id))
      (is (= tuple-tlbl tptrn-nm))
      (is (= tuple-iid instr-id))
      (is (= tuple-ilbl instr-nm)))))

(comment
  (clojure.test/run-tests 'reason-alpha.model.mapping-test)

  )
