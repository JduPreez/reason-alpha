(ns reason-alpha.model.portfolio-management
  (:require [malli.instrument :as malli.instr]
            [reason-alpha.model.core :as model :refer [def-model]]
            [reason-alpha.model.fin-instruments :as model.fin-instruments]))

(def-model TradePattern
  :model/trade-pattern
  [:map
   [:trade-pattern/creation-id uuid?]
   [:trade-pattern/id {:optional true} uuid?]
   [:trade-pattern/name [:string {:min 1}]]
   [:trade-pattern/description {:optional true} [:string {:min 1}]]])

(def Transaction
  [:map
   [:trade-transaction/creation-id uuid?]
   [:trade-transaction/id {:optional true} uuid?]
   [:trade-transaction/type [:enum :buy :sell :dividend :reinvest-divi
                             :corp-action :fee :tax :exchange-fee :stamp-duty]]
   [:trade-transaction/date inst?]
   [:trade-transaction/quantity decimal?]
   [:trade-transaction/price decimal?]
   [:trade-transaction/fee-of-transaction-id {:optional true} uuid?]
   [:trade-transaction/instrument-id uuid?]
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
   [:position/open-trade-transaction TradeTransaction]
   [:position/close-trade-transaction {:optional true} TradeTransaction]
   [:position/dividend-trade-transactions {:optional true} [:sequential TradeTransaction]]
   [:position/instrument-id {:optional true} uuid?]
   [:position/holding-position-id {:optional true} uuid?]
   [:position/account-id uuid?]])

(def PositionDto
  [:map
   [:creation-id uuid?]
   [:id {:optional true} uuid?]
   [:instrument-id uuid?]
   [:instrument-name string?]
   [:quantity decimal?]
   [:symbols {:optional true} string?]
   [:open-price decimal?]
   [:close-price {:optional true} decimal?]
   [:trade-pattern-name {:optional true} string?]
   [:holding-position-id {:optional true} string?]])

#_(defn position-total-return
  "Also know as the Holding Period Yield"
  {:malli/schema
   [:=>
    [:cat
     [:map
      [:position/open-trade-transaction
       [:alt
        [:tuple
         {:description (str "TradeTransaction & it's forex rate to convert to a "
                            "different currency from the transaction's instrument" )}
         TradeTransaction model.fin-instruments/Instrument]
        TradeTransaction]]
      [:position/close-trade-transaction
       [:alt
        [:tuple TradeTransaction model.fin-instruments/Instrument]
        TradeTransaction]]
      [:position/dividend-trade-transactions {:optional true}
       [:sequential [:alt
                     [:tuple TradeTransaction model.fin-instruments/Instrument]
                     TradeTransaction]]]]]
    decimal?]}
  [{:keys [position/open-trade-transaction
           position/close-trade-transaction
           position/dividend-trade-transactions]}]
  (let [{opn-quantity :quantity
         opn-price    :price} (map? open-trade-transaction
                                    open-trade-transaction
                                    (first open-trade-transaction))
        {cls-quantity :quantity
         cls-price    :price} (map? close-trade-transaction
                                    close-trade-transaction
                                    (first open-trade-transaction))
        #_#_divi-txs          (->> dividend-trade-transactions
                                   (map #(if (map? %) % (first %)))
                                   (reduce + ))
        beginning-val         (* opn-quantity opn-price)
        ending-val            (* cls-quantity cls-price)]
    0))

;; TODO: ENABLE THIS WHEN DONE WITH `position-holding-period-return`
;;(malli.instr/collect!)

(comment
  (require '[malli.core :as m]
           '[malli.instrument :as mi]
           '[malli.generator :as mg])

  (position-holding-period-return "kjsj")

  (mg/generate  [:tuple {:title "location"} :double :double])

  (mg/generate [:alt keyword? string?])

  (mg/generate [:alt
                [:tuple TradeTransaction model.fin-instruments/Instrument]
                TradeTransaction])

  (mi/instrument!)

  (m/function-schemas)
  )
