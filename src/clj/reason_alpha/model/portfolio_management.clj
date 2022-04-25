(ns reason-alpha.model.portfolio-management
  (:require [malli.instrument :as malli.instr]
            [reason-alpha.model.core :as model :refer [def-model]]
            [reason-alpha.model.fin-instruments :as fin-instruments]
            [reason-alpha.model.utils :as mutils]))

(def-model TradePattern
  :model/trade-pattern
  [:map
   [:trade-pattern/creation-id uuid?]
   [:trade-pattern/id {:optional true} uuid?]
   [:trade-pattern/parent-id {:optional true} uuid?]
   [:trade-pattern/name [string? {:min 1}]]
   [:trade-pattern/description {:optional true} [string? {:min 1}]]
   [:trade-pattern/account-id {:optional true} uuid?]])

(def-model TradePatternDto
  :model/trade-pattern-dto
  [:map
   [:trade-pattern-creation-id {:command-path [:trade-pattern/creation-id]}
    uuid?]
   [:trade-pattern-id {:optional     true
                       :command-path [:trade-pattern/id]}
    uuid?]
   [:trade-pattern-parent-id {:optional     true
                              :command-path [:trade-pattern/parent-id]}
    uuid?]
   [:trade-pattern-name {:command-path [:trade-pattern/name]}
    [string? {:min 1}]]
   [:trade-pattern-description {:optional     true
                                :command-path [:trade-pattern/description]}
    [string? {:min 1}]]
   [:trade-pattern-account-id {:optional     true
                               :command-path [:trade-pattern/account-id]}
    uuid?]])

(def Transaction
  [:map
   [:trade-transaction/creation-id uuid?]
   [:trade-transaction/id {:optional true} uuid?]
   [:trade-transaction/type [:enum :buy :sell :dividend :reinvest-divi
                             :corp-action :fee :tax :exchange-fee :stamp-duty]]
   [:trade-transaction/date {:optional true} inst?]
   [:trade-transaction/quantity float?]
   [:trade-transaction/price float?]
   [:trade-transaction/fee-of-transaction-id {:optional true} uuid?]
   [:trade-transaction/holding-id uuid?]
   [:trade-transaction/estimated? boolean?]])

;; We have to define entity schemas like this because function schemas
;; don't support recursive references
(def-model TradeTransaction
  :model/trade-transaction
  (conj Transaction
        [:trade-transaction/fee-transactions {:optional true}
         [:sequential Transaction]]))

(def-model Position
  :model/position
  [:map
   [:position/creation-id uuid?]
   [:position/id {:optional true} uuid?]
   [:position/status [:enum :open :closed]]
   [:position/open TradeTransaction]
   [:position/close {:optional true} TradeTransaction]
   [:position/dividends {:optional true} [:sequential TradeTransaction]]
   [:position/holding-id {:optional true} uuid?]
   [:position/account-id uuid?]
   [:position/trade-pattern-id {:optional true} uuid?]
   [:position/long-short
    [:enum {:enum/titles {:long  "Long"
                          :short "Short (Hedge)"}} :long :short]]
   [:position/stop {:optional true} float?]
   [:position/holding-position-id {:optional true} uuid?]])

(def-model PositionDto
  :model/position-dto
  [:map
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
    float?]
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
    float?]
   [:close-price {:title        "Close"
                  :optional     true
                  :command-path [:position/close
                                 :trade-transaction/price]}
    float?]
   [:close-estimated? {:optional     true
                       :command-path [:position/close
                                      :trade-transaction/estimated?]}
    boolean?]
   [:stop {:optional     true
           :title        "Stop"
           :command-path [:position/stop]}
    float?]
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
    uuid?]])

(def-model Holding
  :model/holding
  [:map
   [:holding/creation-id uuid?]
   [:holding/id {:optional true} uuid?]
   [:holding/instrument-name [:string {:min 1}]]
   [:holding/symbols {:optional true} [:sequential fin-instruments/Symbol]]
   [:holding/instrument-type [:enum
                              {:enum/titles {:share  "Share"
                                             :etf    "ETF"
                                             :crypto "Crypto"}}
                      :share :etf :currency :crypto]]
   [:holding/currency fin-instruments/Currency]
   [:holding/prices {:optional true} [:sequential fin-instruments/Price]]
   [:holding/holding-position {:optional true} Position]
   [:holding/positions {:optional true} [:sequential Position]]
   [:holding/account-id {:optional true} uuid?]
   [:holding/target-allocation {:optional true} float?]])

(let [{{ptitles :enum/titles} :properties
       providers              :members} (mutils/get-model-members-of
                                         fin-instruments/Symbol
                                         :symbol/provider)
      symbols-schema                    (for [p    providers
                                              :let [t (get ptitles p)]]
                                          [p {:title        t
                                              :optional     true
                                              :pivot        :symbol/provider
                                              :command-path [:holding/symbols 0 :symbol/ticker]}
                                           string?])]
  (def-model HoldingDto
    :model/holding-dto
    (into
     [:map
      [:holding-id {:optional     true
                    :command-path [:holding/id]}
       uuid?]
      [:holding-creation-id {:command-path [:holding/creation-id]}
       uuid?]
      [:instrument-name {:title        "Instrument"
                         :optional     true
                         :command-path [:holding/instrument-name]} string?]
      [:currency {:title        "Currency"
                  :command-path [[:holding/currency]
                                 [:holding/currency-name]]}
       [:tuple keyword? string?]]]
     cat
     [symbols-schema
      [[:instrument-type {:title        "Type"
                          :optional     true
                          :ref          :holding/instrument-type
                          :command-path [[:holding/instrument-type]
                                         [:holding/instrument-type-name]]}
        [:tuple keyword? string?]]]])))


;; (defn position-total-return
;;   "Also know as the Holding Period Yield"
;;   {:malli/schema
;;    [:=>
;;     [:cat
;;      [:map
;;       [:position/open-trade-transaction
;;        [:alt
;;         [:tuple
;;          {:description (str "TradeTransaction & it's forex rate to convert to a "
;;                             "different currency from the transaction's instrument" )}
;;          TradeTransaction model.fin-instruments/Instrument]
;;         TradeTransaction]]
;;       [:position/close-trade-transaction
;;        [:alt
;;         [:tuple TradeTransaction model.fin-instruments/Instrument]
;;         TradeTransaction]]
;;       [:position/dividend-trade-transactions {:optional true}
;;        [:sequential [:alt
;;                      [:tuple TradeTransaction model.fin-instruments/Instrument]
;;                      TradeTransaction]]]]]
;;     decimal?]}
;;   [{:keys [position/open-trade-transaction
;;            position/close-trade-transaction
;;            position/dividend-trade-transactions]}]
;;   (let [{opn-quantity :quantity
;;          opn-price    :price} (map? open-trade-transaction
;;                                     open-trade-transaction
;;                                     (first open-trade-transaction))
;;         {cls-quantity :quantity
;;          cls-price    :price} (map? close-trade-transaction
;;                                     close-trade-transaction
;;                                     (first open-trade-transaction))
;;         #_#_divi-txs          (->> dividend-trade-transactions
;;                                    (map #(if (map? %) % (first %)))
;;                                    (reduce + ))
;;         beginning-val         (* opn-quantity opn-price)
;;         ending-val            (* cls-quantity cls-price)]
;;     0))
