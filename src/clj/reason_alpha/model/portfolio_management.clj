(ns reason-alpha.model.portfolio-management
  (:require [axel-f.excel :as axel-f]
            [malli.core :as m]
            [malli.instrument :as malli.instr]
            [medley.core :as medley]
            [reason-alpha.model.core :as model :refer [def-model]]
            [reason-alpha.model.fin-instruments :as fin-instruments]
            [reason-alpha.model.utils :as mutils]
            [reason-alpha.utils :as utils]
            [traversy.lens :as lens]))

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
    [:enum {:enum/titles {:long   "Long"
                          :short  "Short"
                          :hedged "Hedged"}}
     :long :short :hedged]]
   [:position/stop {:optional true} number?]
   [:position/target-price {:optional true} number?]
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
     :optional     true
     :command-path [:holding/currency]}
    fin-instruments/Currency]
   [:holding-pos-currency
    {:ref          :position/holding-position
     :optional     true
     :command-path [:holding/holding-position :holding/currency]}
    fin-instruments/Currency]
   [:long-short {:title        "Long/Short/Hedge"
                 :optional     true
                 :ref          :position/long-short
                 :command-path [[:position/long-short]
                                [:position/long-short-name]]}
    [:tuple keyword? string?]]
   [:open-date {:title        "Open Date"
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
   [:open-fx-rate {:optional true} number?]
   [:close-fx-rate {:optional true} number?]
   [:open-holding-pos-fx-rate {:optional true} number?]
   [:close-holding-pos-fx-rate {:optional true} number?]
   ;; `:title` supports Mustache templating and inner HTML too: "Open Total`<br>`({{account-currency-nm}})"
   [:open-total-acc-currency {:title    "Open Total ({{account-currency-nm}})"
                              :optional true
                              :compute  {:function "open-total * open-fx-rate"
                                         :use      [:open-fx-rate]
                                         :require  [:open-total]}}
    number?]
   [:close-price {:title        "Close"
                  :optional     true
                  :command-path [:position/close
                                 :trade-transaction/price]}
    number?]
   [:close-date {:title        "Close Date"
                 :optional     true
                 :command-path [:position/close
                                :trade-transaction/date]}
    [:maybe inst?]]
   [:profit-loss-amount {:title    "Profit/Loss"
                         :optional true
                         :compute  {:function "(quantity * close-price) - open-total"
                                    :use      [:quantity :close-price]
                                    :require  [:open-total]}}
    number?]
   [:profit-loss-amount-acc-currency {:title    "Profit/Loss ({{account-currency-nm}})"
                                      :optional true
                                      :compute  {:function "profit-loss-amount * close-fx-rate"
                                                 :use      [:close-fx-rate]
                                                 :require  [:profit-loss-amount]}}
    number?]
   [:profit-loss-percent {:title    "Profit/Loss %"
                          :optional true
                          :compute  {:function "TPERCENT(profit-loss-amount / open-total)"
                                     :require  [:open-total :profit-loss-amount]}}
    [:maybe string?]]
   ;; Profit/Loss Long
   ;; Profit/Loss Long Main Currency
   ;; Profit/Loss Long %
   [:target-price {:title        "Target Price"
                   :optional     true
                   :command-path [:position/target-price]}
    number?]
   [:target-profit {:title    "Target Profit"
                    :optional true
                    :compute  {:function "(target-price * quantity) - open-total"
                               :use      [:target-price :quantity]
                               :require  [:open-total]}}
    [:maybe number?]]
   [:target-profit-acc-currency {:title    "Target Profit ({{account-currency-nm}})"
                                 :optional true
                                 :compute  {:function "target-profit * close-fx-rate"
                                            :use      [:close-fx-rate]
                                            :require  [:target-profit]}}
    [:maybe number?]]
   [:target-profit-percent {:title    "Target Profit %"
                            :optional true
                            :compute  {:function "TPERCENT(target-profit / open-total)"
                                       :require  [:target-profit :open-total]}}
    [:maybe string?]]
   [:status {:optional     true
             :command-path [:position/status]}
    keyword?]
   [:stop {:title        "Stop"
           :optional     true
           :command-path [:position/stop]}
    [:maybe number?]]
   [:stop-loss {:title    "Stop Loss"
                :optional true
                :compute  {:function "(stop * quantity) - open-total"
                           :use      [:stop :quantity]
                           :require  [:open-total]}}
    [:maybe number?]]
   [:stop-loss-acc-currency {:title    "Stop Loss ({{account-currency-nm}})"
                             :optional true
                             :compute  {:function "stop-loss * close-fx-rate"
                                        :use      [:close-fx-rate]
                                        :require  [:stop-loss]}}
    [:maybe number?]]
   [:stop-loss-percent {:optional true
                        :title    "Stop Loss % of Allocation"
                        :compute  {:function "TPERCENT(stop-loss/(quantity * open-price))"
                                   :use      [:stop-loss :quantity :open-price]}}
    [:maybe string?]]
   ;; 1st Deviation
   ;; 1st Deviation Amount Open
   ;; 1st Deviation Amount Close
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
    uuid?]
   ;; IDs of child positions. Used to determine if a row `:can-edit`
   [:sub-positions {:optional true}
    [:maybe [:sequential uuid?]]]])

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

(defn idx-position-type
  [positions]
  (let [holding-pos (-> positions
                        (lens/view
                         (lens/only
                          #(nil?
                            (:holding-position-id %))))
                        first)
        positions   (-> positions
                        (lens/view
                         (lens/only
                          :holding-position-id)))]
    {:holding-position holding-pos
     :positions        positions}))

(defn aggregate-holding-position
  [{:keys [stop open-price quantity long-short
           open-date close-date]
    :as   position} sub-positions]

   ;; If the stop/quantity is set on the overall holding position,
   ;; then assume it's manually managed & don't do a roll-up summary.
  (let [sub-positions      (or (seq sub-positions) '())
        sub-pos-count      (count sub-positions)
        ;; When the sub-positions have different currencies, we need to calculate
        ;; open-price values using the account-currency
        ;; TODO: When the sub-positions all have the same currency, then use the position's
        ;; currency instead.
        total-quantity     (if (> sub-pos-count 1)
                             (->> sub-positions
                                  (reduce
                                   (fn [{q1      :quantity
                                         [ls1 _] :long-short
                                         :as     total-q} {q2      :quantity
                                                           [ls2 _] :long-short}]
                                     (let [q1 (if (map? total-q)
                                                (if (= :long ls1)
                                                  (or q1 0) #_else (* -1 q1))
                                                #_else total-q)
                                           q2 (or q2 0)
                                           q2 (if (= :long ls2)
                                                q2 #_else (* -1 q2))]
                                       (+ q1 q2)))))
                             #_else
                             (let [{ls :long-short
                                    q  :quantity} (first sub-positions)
                                   q              (or q 0)]
                               (if (= :long ls)
                                 q #_else (* -1 q))))
        overall-open-total (if (> sub-pos-count 1)
                             (->> sub-positions
                                  (reduce
                                   (fn [{open1   :open-total-acc-currency
                                         [ls1 _] :long-short
                                         :as     overall-open} {open2   :open-total-acc-currency
                                                                [ls2 _] :long-short}]
                                     (let [op1 (if (map? overall-open)
                                                 (if (= :long ls1)
                                                   (or open1 0) #_else (* -1 open1))
                                                 overall-open)
                                           op2 (or open2 0)
                                           op2 (if (= :long  ls2)
                                                 op2 #_else (* -1 op2))]
                                       (+ op1 op2)))))
                             #_else
                             (let [{ls :long-short
                                    ot :open-total-acc-currency} (first sub-positions)
                                   ot                            (or ot 0)]
                               (if (= :long ls)
                                 ot #_else (* -1 ot))))
        overall-open-price (when (not= total-quantity 0)
                             (/ (double overall-open-total) (double total-quantity)))
        ;; TODO P/L-acc-currency: Sum each position's P/L in account currency
        ;; TODO P/L %: Divide P/L-acc-currency by the `overall-open-total`
        sub-pids           (->> sub-positions
                                (map #(or (:poition-id %)
                                          (:position-creation-id %))))
        ls-type            (if (every? #(= :long (:long-short %))
                                       sub-positions)
                             :long #_else [:hedged ""])
        open-date          (->> sub-positions
                                (apply medley/least-by :open-date)
                                :open-date)
        close-date         (->> sub-positions
                                (apply medley/greatest-by :close-date)
                                :close-date)
        ;; We want to avoid summing `:stop-loss` up as zero, if none
        ;; of the child positions have a `:stop-loss` amount.
        ;; Therefore check that at least one child as a stop amount
        stop-loss          (when (some :stop-loss sub-positions)
                             (reduce #(+ (or (:stop-loss %1) 0)
                                         (or (:stop-loss %2) 0)) sub-positions))
        position           (assoc position
                                  :open-price              overall-open-price
                                  :open-total              overall-open-total
                                  :open-total-acc-currency overall-open-total
                                  :stop                    nil
                                  :quantity                total-quantity
                                  :sub-positions           sub-pids
                                  :long-short              ls-type
                                  :open-date               open-date
                                  :close-date              close-date
                                  :stop-loss               stop-loss)]
    {:result position
     :type   :success}))

(comment
  (let [ps                        [{:open-total-acc-currency         500,
                                    :trade-pattern
                                    [#uuid "018a2c43-7a96-11a5-ab21-06f37976bbf8" "Breakout"],
                                    :target-profit-acc-currency      nil,
                                    :stop-loss-acc-currency          nil,
                                    :target-profit                   nil,
                                    :holding                         [#uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197" "Adyen"],
                                    :profit-loss-amount              -21.729999999999563,
                                    :open-total                      31333.43,
                                    :open-price                      764.23,
                                    :profit-loss-percent             "-0.07%",
                                    :stop-loss                       -31333.43,
                                    :holding-currency                :EUR,
                                    :stop                            0,
                                    :marketstack                     "ADYEN.XAMS",
                                    :stop-loss-percent               "-100.0%",
                                    :position-creation-id            #uuid "d561b5c7-57ab-49b0-84ce-2d04b78f588c",
                                    :status                          :open,
                                    :close-price                     763.7,
                                    :position-id                     #uuid "018a2cac-c474-3ec7-962c-b5b285877385",
                                    :open-date                       #inst "2023-08-24T00:00:00.000-00:00",
                                    :holding-position-id             #uuid "018a2caa-ba6e-c9a5-8d51-38553003af1f",
                                    :holding-id                      #uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197",
                                    :quantity                        41,
                                    :profit-loss-amount-acc-currency nil,
                                    :long-short                      [:long ""],
                                    :target-profit-percent           nil}
                                   {:open-total-acc-currency         500,
                                    :trade-pattern
                                    [#uuid "018a2c43-7a96-11a5-ab21-06f37976bbf8" "Breakout"],
                                    :target-profit-acc-currency      nil,
                                    :stop-loss-acc-currency          nil,
                                    :target-profit                   nil,
                                    :holding                         [#uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197" "Adyen"],
                                    :profit-loss-amount              -21.729999999999563,
                                    :open-total                      31333.43,
                                    :open-price                      764.23,
                                    :profit-loss-percent             "-0.07%",
                                    :stop-loss                       -31333.43,
                                    :holding-currency                :EUR,
                                    :stop                            0,
                                    :marketstack                     "ADYEN.XAMS",
                                    :stop-loss-percent               "-100.0%",
                                    :position-creation-id            #uuid "cf9da077-0d0c-40d6-b570-f3edd056ca79",
                                    :status                          :open,
                                    :close-price                     763.7,
                                    :position-id                     #uuid "4b463c08-c0ce-4178-b5b8-1ebce5b7e53a",
                                    :open-date                       #inst "2023-08-24T00:00:00.000-00:00",
                                    :holding-position-id             #uuid "018a2caa-ba6e-c9a5-8d51-38553003af1f",
                                    :holding-id                      #uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197",
                                    :quantity                        5,
                                    :profit-loss-amount-acc-currency nil,
                                    :long-short                      [:long ""],
                                    :target-profit-percent           nil}
                                   {:open-total-acc-currency         nil,
                                    :target-profit-acc-currency      nil,
                                    :stop-loss-acc-currency          nil,
                                    :target-profit                   nil,
                                    :holding                         [#uuid "018a465e-82bd-de65-2623-02bb30e1a1f6" "Multiple"],
                                    :profit-loss-amount              375.9708007812478,
                                    :open-total                      31333.42919921875,
                                    :open-price                      764.23,
                                    :profit-loss-percent             "1.2%",
                                    :stop-loss                       -31333.42919921875,
                                    :holding-currency                :SGD,
                                    :stop                            0.0,
                                    :stop-loss-percent               "-100.0%",
                                    :sub-positions                   '(#uuid "d561b5c7-57ab-49b0-84ce-2d04b78f588c"),
                                    :position-creation-id            #uuid "3cee7b50-68a9-4fa3-b9eb-5464068ad465",
                                    :status                          :open,
                                    :close-price                     773.4,
                                    :close-date                      nil,
                                    :position-id                     #uuid "018a2caa-ba6e-c9a5-8d51-38553003af1f",
                                    :open-date                       #inst "2023-08-24T00:00:00.000-00:00",
                                    :holding-id                      #uuid "018a465e-82bd-de65-2623-02bb30e1a1f6",
                                    :quantity                        41,
                                    :profit-loss-amount-acc-currency nil,
                                    :long-short                      [:hedged ""],
                                    :target-profit-percent           nil}]
        {:keys             [holding-position
                positions] :as x} (idx-position-type ps)]
    (aggregate-holding-position holding-position positions))

  )

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
