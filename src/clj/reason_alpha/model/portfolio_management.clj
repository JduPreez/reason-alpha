(ns reason-alpha.model.portfolio-management
  (:require [axel-f.excel :as axel-f]
            [malli.core :as m]
            [malli.instrument :as malli.instr]
            [reason-alpha.model.core :as model :refer [def-model]]
            [reason-alpha.model.fin-instruments :as fin-instruments]
            [reason-alpha.model.utils :as mutils]
            [reason-alpha.utils :as utils]))

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
   [:trade-pattern-name {:title        "Trade Pattern"
                         :command-path [:trade-pattern/name]}
    [string? {:min 1}]]
   [:trade-pattern-description {:title        "Description"
                                :optional     true
                                :command-path [:trade-pattern/description]}
    [string? {:min 1}]]
   [:trade-pattern-parent-id {:title        "Sub-Pattern"
                              :optional     true
                              :ref          :trade-pattern/parent
                              :command-path [:trade-pattern/parent-id]}
    uuid?]])

(def Transaction
  [:map
   [:trade-transaction/creation-id {:optional true} uuid?]
   [:trade-transaction/id {:optional true} uuid?]
   [:trade-transaction/type [:enum :buy :sell :dividend :reinvest-divi
                             :corp-action :fee :tax :exchange-fee :stamp-duty]]
   [:trade-transaction/date {:optional true} inst?]
   [:trade-transaction/quantity number?]
   [:trade-transaction/price number?]
   [:trade-transaction/fee-of-transaction-id {:optional true} uuid?]
   [:trade-transaction/holding-id uuid?]])

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
   [:position/holding-id uuid?]
   [:position/account-id uuid?]
   [:position/trade-pattern-id {:optional true} uuid?]
   [:position/long-short
    [:enum {:enum/titles {:long  "Long"
                          :short "Short (Hedge)"}} :long :short]]
   [:position/stop {:optional true} number?]
   [:position/holding-position-id {:optional true} uuid?]])

(comment
  (let [f-str (:stop-percent-loss position-dto-formulas)
        f-str (format "WITH(PERCENT, FN(n, ROUND(n * 100, 2)),
                            TPERCENT, FN(n, PERCENT(n) & '%%'), %s)" f-str)
        f     (axel-f/compile f-str)
        data  {:stop-loss  -760
               :quantity   152
               :open-price 71.83}]
    (f data))
  
)

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
   [:holding-currency
    {:ref          :holding
     :command-path [:holding/currency]}
    fin-instruments/Currency]
   [:long-short {:title        "Long/Short (Hedge)"
                 :ref          :position/long-short
                 :command-path [[:position/long-short]
                                [:position/long-short-name]]}
    [:tuple keyword? string?]]
   [:open-time {:title        "Open Date"
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
   [:fx-rate
    number?]
   ;; `:title` supports Mustache templating and inner HTML too: "Open Total`<br>`({{account-currency-nm}})"
   [:open-total-acc-currency {:title    "Open Total ({{account-currency-nm}})"
                              :optional true
                              :compute  {:function "quantity * open-price"
                                         :use      [:quantity :open-price]}}
    number?]
   [:close-price {:title        "Close"
                  :optional     true
                  :command-path [:position/close
                                 :trade-transaction/price]}
    number?]
   ;; Close date
   [:profit-loss-amount {:title    "Profit/Loss"
                         :optional true
                         :compute  {:function "(quantity * close-price) - open-total"
                                    :use      [:quantity :close-price]
                                    :require  [:open-total]}}
    number?]
   ;; Profit/Loss Amount Main Currency
   ;; Profit/Loss %
   ;; Profit/Loss Long
   ;; Profit/Loss Long Main Currency
   ;; Profit/Loss Long %
   ;; Target Price
   ;; Target Profit
   ;; Target Profit Main Currency
   ;; Target Profit %
   [:status {:optional     true
             :command-path [:position/status]}
    keyword?]
   [:stop {:optional     true
           :title        "Stop"
           :command-path [:position/stop]}
    number?]
   [:stop-loss {:title    "Stop Loss"
                :optional true}
    number?]
   ;; Stop Loss Main Currency
   [:stop-loss-percent {:optional true
                        :title    "Stop Loss % of Allocation"
                        :compute  {:function "TPERCENT(stop-loss/(quantity * open-price))"
                                   :use      [:stop-loss :quantity :open-price]}}
    string?]
   ;; 1st Deviation
   ;; 1st Deviation Amount Open
   ;; 1st Deviation Amount Close
   ;; Exchange Rate Main Currency
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
   [:marketstack {:optional     true
                  :fn-value     {:arg :symbol/provider
                                 :fun '(fn [{p :symbol/provider
                                             v :symbol/ticker}]
                                         (when (= p :marketstack)
                                           {:value v}))}
                  :command-path [:holding/symbols 0 :symbol/ticker]}
    string?]
   [:holding-id {:optional     true
                 :command-path [:position/holding-id]}
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
       providers              :members} (mutils/model-member-schema-info
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
                :ref          :holding/currency
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

(defn assoc-aggregate-fields
  ([{:keys [stop open-price quantity] :as position}]
   (or (and stop
            (-> stop
                (- open-price)
                (* quantity)
                utils/round
                (as-> a (assoc position :stop-loss a))))
       position))
  ([{:keys [stop open-price quantity] :as position}
    sub-positions]

   ;; If the stop/quantity is set on the overall holding position,
   ;; then assume it's manually managed & don't do a roll-up summary.
   (let [fn-avg-cost    (fn [quantity amount]
                          (when (and quantity
                                     amount
                                     (not= 0 quantity))
                            (/ amount quantity)))
         total-quantity (or quantity
                            (->> sub-positions
                                 (reduce #(+ (or %1 0) (or %2 0)))))
         avg-cost-open  (or open-price
                            (->> sub-positions
                                 (map (fn [{:keys [open-price quantity]}]
                                        (* (or open-price 0)
                                           (or quantity 0))))
                                 (reduce +)
                                 (fn-avg-cost total-quantity)))
         ;; TODO: Remove this
         avg-cost-open  (if (string? avg-cost-open)
                          (read-string avg-cost-open)
                          avg-cost-open)
         avg-cost-stop  (or stop
                            (->> sub-positions
                                 (map (fn [{:keys [stop quantity]}]
                                        (* (or stop 0)
                                           (or quantity 0))))
                                 (reduce +)
                                 (fn-avg-cost total-quantity)))

         ;; TODO: Remove this
         avg-cost-stop (if (string? avg-cost-stop)
                         (read-string avg-cost-stop)
                         avg-cost-stop)
         ;; TODO: When we add support for short positions, the holding position's
         ;;       `:stop-loss` should be simple sum of sub positions `:stop-loss`
         ;;       and the `:stop`'s `avg-cost-stop` should be derived only from the
         ;;       long sub positions.
         position      (-> position
                           (merge {:open-price avg-cost-open
                                   :stop       avg-cost-stop
                                   :quantity   total-quantity})
                           assoc-aggregate-fields
                           (merge {:open-price (utils/round avg-cost-open)
                                   :stop       (utils/round avg-cost-stop)
                                   :quantity   total-quantity}))]
     position)))

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
