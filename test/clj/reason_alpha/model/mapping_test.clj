(ns reason-alpha.model.mapping-test
  (:require  [clojure.test :refer :all]
             [reason-alpha.model.mapping :as sut]
             [reason-alpha.utils :as utils]))

(def currency-acc-path [:instrument/currency-instrument :instrument/account :account/user-name])

(def holding-qry-dto-model [:map
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
                              :command-path [[:instrument/type]
                                             [:instrument/type-name]]}
                             [:tuple keyword string?]]
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
                            [:easy-equities
                             {:title        "Easy Equities",
                              :optional     true,
                              :pivot        :symbol/provider,
                              :command-path [:instrument/symbols 0 :symbol/ticker]}
                             string?]
                            [:currency-account
                             {:title        "Currency Account"
                              :optional     true
                              :command-path currency-acc-path}]])

(def position-qry-dto-model [:map
                             [:position-creation-id {:command-path [:position/creation-id]}
                              uuid?]
                             [:position-id {:optional     true
                                            :command-path [:position/id]} uuid?]
                             [:holding {:title        "Holding (Instrument)"
                                        :ref          :holding
                                        :command-path [[:position/holding-id]
                                                       [:holding/instrument-name]]}
                              [:tuple uuid? string?]]
                             [:quantity {:title        "Quantity"
                                         :command-path [:position/open
                                                        :trade-transaction/quantity]}
                              number?]
                             [:long-short {:title        "Long/Short (Hedge)"
                                           :ref          :position/long-short
                                           :command-path [[:position/long-short]
                                                          [:position/long-short-name]]}
                              [:tuple keyword? string?]]
                             [:open-time {:title        "Open Time"
                                          :command-path [:position/open
                                                         :trade-transaction/date]}
                              inst?]
                             [:open-price {:title        "Open"
                                           :command-path [:position/open
                                                          :trade-transaction/price]}
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
                             [:stop-total-loss {:title    "Stop Total Loss"
                                                :optional true}
                              float?]
                             [:eod-historical-data {:optional     true
                                                    :fn-value     {:arg :symbol/provider
                                                                   :fun '(fn [{p :symbol/provider
                                                                               v :symbol/ticker}]
                                                                           (when (= p :eod-historical-data)
                                                                             {:value v}))}
                                                    :command-path [:holding/symbols 0 :symbol/ticker]}
                              string?]])

(deftest mapping-holding-query-dto->command-ent
  (let [{:keys [instrument-id
                instrument-creation-id
                instrument-type
                instrument-name
                saxo-dma
                yahoo-finance
                currency-account]
         :as   query-dto} {:instrument-id          (utils/new-uuid)
                           :instrument-creation-id (utils/new-uuid)
                           :instrument-type        [:share ""]
                           :instrument-name        "Tencent"
                           :saxo-dma               "00700:xhkg"
                           :yahoo-finance          "0700.hk"
                           :currency-account       "ark@ark.com"}
        {:keys [instrument/creation-id
                instrument/id
                instrument/type
                instrument/name
                instrument/symbols]
         :as   cmd-ent}   (sut/query-dto->command-ent holding-qry-dto-model
                                                      query-dto)]
    (testing "`query-dto->command-ent` should map non-nested path"
      (is (= id instrument-id))
      (is (= creation-id instrument-creation-id))
      (is (= type (first instrument-type)))
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

(deftest mapping-position-command-ent->query-dto
  (let [cid                           #uuid "74e921b2-fe79-454d-b47c-08c43b298019"
        id                            #uuid "0180098b-e65e-d7ce-645b-41eef737fa0c"
        hid                           #uuid "018004bb-227c-d6de-69ac-bc1eab688ab5"
        hname                         "Duolingo"
        qty                           150
        l-s                           :long
        otime                         #inst "2022-04-06T00:00:00.000-00:00"
        oprice                        352.22
        cprice                        421.34
        st                            :open
        stp                           325
        trade-ptrn-id                 #uuid "01800865-9069-63c7-9c6c-4d24cdcefc9a"
        trade-ptrn-nm                 "Breakout"
        hposition-id                  #uuid "018008e1-a0db-2638-b2e2-4e9f0e332d11"
        eod                           "XOXOXO"
        root+ref-ents                 [{:position/holding-position-id hposition-id,
                                        :position/trade-pattern-id    trade-ptrn-id,
                                        :position/long-short          l-s,
                                        :position/id                  id,
                                        :position/creation-id         cid,
                                        :position/status              st
                                        :position/open
                                        #:trade-transaction{:quantity qty,
                                                            :date     otime,
                                                            :price    oprice},
                                        :position/close               #:trade-transaction{:price cprice,},
                                        :position/holding-id          hid,
                                        :xt/id                        #uuid "0180098b-e65e-d7ce-645b-41eef737fa0c",
                                        :position/account-id          #uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49",
                                        :position/stop                stp}
                                       {:holding/creation-id     #uuid "c818b4ab-fbc7-48ca-ab7b-abf15a71b74c",
                                        :holding/instrument-name hname,
                                        :holding/symbols
                                        [#:symbol{:ticker eod :provider :eod-historical-data}
                                         #:symbol{:ticker "BBB", :provider :saxo-dma}
                                         #:symbol{:ticker "BBB", :provider :easy-equities}],
                                        :holding/instrument-type :crypto,
                                        :holding/account-id      #uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49",
                                        :holding/id              hid,
                                        :xt/id                   #uuid "018004bb-227c-d6de-69ac-bc1eab688ab5"}
                                       {:trade-pattern/creation-id #uuid "47427389-60f3-4f0b-a32e-7fbe139b6e36",
                                        :trade-pattern/id          #uuid "01800865-9069-63c7-9c6c-4d24cdcefc9a",
                                        :trade-pattern/name        trade-ptrn-nm,
                                        :trade-pattern/description "Lorem ipsum",
                                        :trade-pattern/account-id  #uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49",
                                        :xt/id                     #uuid "01800865-9069-63c7-9c6c-4d24cdcefc9a"}]
        {:keys [position-creation-id
                position-id
                holding
                quantity
                long-short
                open-time
                open-price
                close-price
                status
                stop
                trade-pattern
                holding-position-id
                eod-historical-data]} (sut/command-ent->query-dto position-qry-dto-model root+ref-ents)]
    (testing "`command-ent->query-dto` should map using a function"
      (is (= eod eod-historical-data)))
    (testing "`command-ent->query-dto` should map simple command paths"
      (is (= id position-id))
      (is (= cid position-creation-id))
      (is (= hid (first holding)))
      (is (= qty quantity))
      (is (= otime open-time))
      (is (= oprice open-price))
      (is (= cprice close-price))
      (is (= st status))
      (is (= stp stop))
      (is (= hposition-id holding-position-id)))
    (testing "`command-ent->query-dto` should map tuples"
      (is (= [hid hname] holding))
      (is (= [l-s ""] long-short))
      (is (= [trade-ptrn-id trade-ptrn-nm] trade-pattern)))))

(deftest mapping-command-ents->query-dtos
  (let [symbol-yahoo-ticker     "SBUX"
        symbol-ee-ticker        "SBUX.NY"
        {:keys [instrument/id
                instrument/creation-id]
         iname :instrument/name
         :as   ent}             {:instrument/id   #uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc",
                                 :instrument/creation-id
                                 #uuid "ea669a4e-e815-4100-8cf3-da7d7fa50a17",
                                 :instrument/name "Starbucks",
                                 :instrument/symbols
                                 [{:symbol/ticker   symbol-yahoo-ticker
                                   :symbol/provider :yahoo-finance}
                                  {:symbol/ticker   symbol-ee-ticker
                                   :symbol/provider :easy-equities}],
                                 :instrument/type :share}
        {:keys [instrument-id instrument-creation-id
                instrument-name yahoo-finance
                easy-equities]} (first (sut/command-ents->query-dtos holding-qry-dto-model
                                                                     [[ent]]))]
    (testing "`command-ents->query-dtos` should map non-pivot fields"
      (is (= id instrument-id))
      (is (= creation-id instrument-creation-id))
      (is (= iname instrument-name))
      (is (= yahoo-finance symbol-yahoo-ticker))
      (is (= easy-equities symbol-ee-ticker)))))

(comment
  (let [ent {:instrument/id   #uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc",
             :instrument/creation-id
             #uuid "ea669a4e-e815-4100-8cf3-da7d7fa50a17",
             :instrument/name "Starbucks",
             :instrument/symbols
             [{:symbol/ticker   "SBUX"
               :symbol/provider :yahoo-finance}
              {:symbol/ticker   "SBUX.NY"
               :symbol/provider :easy-equities}],
             :instrument/type :share}]
    (sut/command-ents->query-dtos holding-qry-dto-model
                                  [[ent]]))

  )

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
