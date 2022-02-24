(ns reason-alpha.views.positions
  (:require [reason-alpha.views.datagrid :as datagrid]
            [reason-alpha.views.instruments :as views.instruments]
            [reason-alpha.views.trade-patterns :as views.trade-patterns]))

(def fields
  [{:title    "Instrument"
    :name     :instrument-name
    :can-sort true
    :menu     [{:title "Edit"
                :view  ::views.instruments/view}]}
   {:title "Quantity"
    :name  :trade-transaction-quantity}
   {:title "Open Time"
    :name  :trade-transaction-date}
   {:title "Open"
    :name  :open-price}
   {:title "Close"
    :name  :close-price}
   {:title "Trade Pattern"
    :name  :trade-pattern-name
    :menu  [{:title "Edit"
             :view  ::views.trade-patterns/view}]}])

(def options
  {:grid-id           ::view
   :title             "Positions"
   :data-subscription [:positions]
   :id-field          :position/id
   :can-sort          true})

(defn view []
  [datagrid/view fields options])
