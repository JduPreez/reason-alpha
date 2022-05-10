(ns reason-alpha.model.portfolio-management
  (:require [malli.core :as m]
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
                              :ref          :trade-pattern
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
    float?]])

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

(defn assoc-stop-total-loss
  ([{:keys [stop open-price quantity] :as position}]
   ;; TODO: Remove this conversion once validation has been fixed
   (let [stop       (if (string? stop)
                      (read-string stop)
                      stop)
         open-price (if (string? open-price)
                      (read-string open-price)
                      open-price)
         quantity   (if (string? quantity)
                      (read-string quantity)
                      quantity)]
     (-> stop
         (- open-price)
         (* quantity)
         utils/round
         (as-> a (assoc position :stop-total-loss a)))))
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
         position      (-> position
                           (merge {:open-price avg-cost-open
                                   :stop       avg-cost-stop
                                   :quantity   total-quantity})
                           assoc-stop-total-loss
                           (merge {:open-price (utils/round avg-cost-open)
                                   :stop       (utils/round avg-cost-stop)
                                   :quantity   total-quantity}))]
     position)))

(comment

  (let [fn-avg-cost    (fn [quantity amount]
                         (when (and quantity
                                    amount
                                    (not= 0 quantity))
                           (/ amount quantity)))
        open-price     (rand 100)
        holding-pos-id (utils/new-uuid)
        holding-pos    {:position-creation-id (utils/new-uuid)
                        :position-id          holding-pos-id
                        :long-short           :long}
        sub-positions  [{:position-creation-id (utils/new-uuid)
                         :position-id          (utils/new-uuid)
                         :holding-position-id  holding-pos-id
                         :long-short           :long
                         :quantity             35.0
                         :open-price           73.77
                         :stop                 68.5}
                        {:position-creation-id (utils/new-uuid)
                         :position-id          (utils/new-uuid)
                         :holding-position-id  holding-pos-id
                         :long-short           :long
                         :quantity             35.0
                         :open-price           61.04}
                        {:position-creation-id (utils/new-uuid)
                         :position-id          (utils/new-uuid)
                         :holding-position-id  holding-pos-id
                         :long-short           :long
                         :quantity             30.0
                         :open-price           78.25
                         :stop                 70.0}
                        {:position-creation-id (utils/new-uuid)
                         :position-id          (utils/new-uuid)
                         :holding-position-id  holding-pos-id
                         :long-short           :long
                         :quantity             30.0
                         :open-price           57.84}]
        total-quantity (->> sub-positions
                            (map :quantity)
                            (remove nil?)
                            (reduce +))]
    (assoc-stop-total-loss holding-pos sub-positions)
    #_(->> sub-positions
           (reduce (fn [total {:keys [stop quantity]}]
                   (if stop
                     (-> quantity
                         (* stop)
                         (+ (or total 0)))
                     
                     total)) nil))
    )

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
