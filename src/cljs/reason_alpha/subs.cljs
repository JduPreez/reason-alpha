(ns reason-alpha.subs
  (:require [cljs-time.core :as t]
            [re-frame.core :as rf]
            [reason-alpha.data.trade-patterns :as data.trade-patterns]
            [reason-alpha.utils :as utils]))

;; Trade patterns

(rf/reg-sub
 :trade-patterns
 (fn [db _]
   (sort-by :trade-pattern/name
            (get-in db data.trade-patterns/root))))

(rf/reg-sub
 :trade-pattern-options
 :<- [:trade-patterns]
 (fn [trade-patterns]
   (let [opts (->> trade-patterns
                   (filter (fn [{:keys [trade-pattern/parent-id]
                                 :as   tp}]
                             ;; Only return parent/non-child trade-patterns
                             (when (not parent-id) tp)))
                   (map (fn [{:keys [trade-pattern/id
                                     trade-pattern/name]}]
                          {:label name
                           :value id})))]
     opts)))

(rf/reg-sub
 :trade-patterns/ref-data
 :<- [:trade-patterns]
 (fn [trade-patterns]
   (let [ref-data (->> trade-patterns
                       (filter (fn [{:keys [trade-pattern/parent-id]
                                     :as   tp}]
                                 ;; Only return parent/non-child trade-patterns
                                 (when (not parent-id) tp)))
                       (map (fn [{:keys [trade-pattern/id
                                         trade-pattern/name]}]
                              [(str id) name]))
                       (into {}))]
     (cljs.pprint/pprint {:trade-patterns/ref-data {:RD  ref-data
                                                    :TPS trade-patterns}})
     ref-data)))

;; Trades

(def securities #{"IGG"
                  "SHA"
                  "CH8"
                  "CTSH"
                  "DIS"
                  "FB"
                  "GILD"
                  "GLD"
                  "GSK"
                  "INTC"
                  "PTEC"})

(defn get-trades [quantity]
  (for [day (range quantity)]
    {:trade/id                             day
     :open-date                            (t/minus (t/now) (t/days day))
     :trade-pattern                        "Breakout - Entering Preceding Base"
     :security                             "ZZZ" #_(securities day)
     :long-short                           (if (odd? day) "L" "S")
     :trading-time-frame                   "Day"
     :quantity                             (utils/rand-between 100 100000)
     :open                                 (utils/rand-between 1 300)
     :close                                (utils/rand-between 1 300)
     :currency                             "USD"
     :cost                                 (utils/rand-between 5 20)
     :interest-per-day                     (utils/rand-between 1 10)
     :interest-total                       (utils/rand-between 1 10)
     :total-cost                           (utils/rand-between 10 30)
     :profit-target                        (utils/rand-between 1 300)
     :profit-target-total                  (utils/rand-between 1 1000)
     :profit-target-percent                (utils/rand-between 1 100)
     :profit-target-total-incl-costs       (utils/rand-between 1 1000)
     :profit-target-percent-incl-costs     (utils/rand-between 1 100)
     :close-date                           (t/now)
     :profit-loss                          (utils/rand-between -1000 1000)
     :profit-loss-incl-costs               (utils/rand-between -1000 1000)
     :profit-loss-home-currency            (utils/rand-between -1000 1000)
     :profit-loss-incl-costs-home-currency (utils/rand-between -1000 1000)
     :profit-loss-percent-risk             (utils/rand-between -100 100)
     :stop                                 (utils/rand-between 1 300)
     :stop-loss-total                      (utils/rand-between -300 300)
     :loss-percent                         (utils/rand-between -100 1)
     :first-deviation                      (rand 100)
     :first-deviation-stop                 (rand 100)
     :conversion-rate-home-currency        (rand 100)}))

(def trades-data (delay (get-trades 3)))

(rf/reg-sub
 :trades
 (fn [_ _]
   (cljs.pprint/pprint {:trades @trades-data})
   @trades-data))

(comment
  (into {} [["test1" 1]
            ["test2" 2]
            ["test3" 3]])
  )

;; System

(rf/reg-sub
 :active-view-model     ;; usage: (subscribe [:active-view])
 (fn [db _]             ;; db is the (map) value stored in the app-db atom
   (:active-view-model db)))  ;; extract a value from the application state

(rf/reg-sub
 :loading
 (fn [db [_ key']]
   {key' (true?
          (get-in db [:loading key']))}))
