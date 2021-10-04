(ns reason-alpha.views.trades
  (:require [reason-alpha.views.datagrid :as datagrid]))

(def fields
  [#_{:title "Open Date"
      :name  :open-date}
   {:title "Trade Pattern"
    :name  :trade-pattern}
   {:title    "Security"
    :name     :security
    :can-sort true}
   #_{:title "Long/Short"
      :name  :long-short}
   #_{:title "Trading Time Frame"
      :name  :trading-time-frame}
   {:title "Quantity"
    :name  :quantity}
   #_{:title "Open"
      :name  :open}
   #_{:title "Close"
      :name  :close}
   #_{:title "Currency"
      :name  :currency}
   #_{:title "Cost"
      :name  :cost}
   #_{:title "Interest/ Day"
      :name  :interest-per-day}
   #_{:title "Interest Total"
      :name  :interest-total}
   #_{:title "Total Cost"
      :name  :total-cost}
   #_{:title "Profit Target"
      :name  :profit-target}
   #_{:title "Profit Target Total"
      :name  :profit-target-total}
   #_{:title "Profit Target %"
      :name  :profit-target-percent}
   #_{:title "Profit Target Total Incl. Costs"
      :name  :profit-target-total-incl-costs}
   #_{:title "Profit Target % Incl. Costs"
      :name  :profit-target-percent-incl-costs}
   #_{:title "Close Date"
      :name  :close-date}
   #_{:title "Profit/Loss"
      :name  :profit-loss}
   #_{:title "Profit/Loss Incl. Costs"
      :name  :profit-loss-incl-costs}
   #_{:title "Profit/Loss (Home Currency)"
      :name  :profit-loss-home-currency}
   #_{:title "Profit/Loss Incl. Costs (Home Currency)"
      :name  :profit-loss-incl-costs-home-currency}
   #_{:title "Profit/Loss % Risk"
      :name  :profit-loss-percent-risk}
   #_{:title "stop"
      :name  :stop}
   #_{:title "Stop Loss Total"
      :name  :stop-loss-total}
   #_{:title "Loss %"
      :name  :loss-percent}
   #_{:title "1st Deviation"
      :name  :first-deviation}
   #_{:title "1st Deviation Stop"
      :name  :first-deviation-stop}
   #_{:title "Conversion Rate (Home Currency)"
      :name  :conversion-rate-home-currency}])

(def options
  {:data-subscription [:trades]
   :grid-id           :trades
   :id-field          :trade/id
   :can-sort          true})

(defn view []
  [datagrid/view fields options])
