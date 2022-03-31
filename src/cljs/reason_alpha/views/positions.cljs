(ns reason-alpha.views.positions
  (:require [re-frame.core :as rf]
            [reason-alpha.views.datagrid :as datagrid]
            [reason-alpha.views.instruments :as views.instruments]
            [reason-alpha.views.trade-patterns :as views.trade-patterns]))

#_(def fields
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
   :data-subscription [:position/list]
   :id-field          :position-creation-id
   :create-dispatch   [:position.command/create]
   :update-dispatch   [:position.command/update]
   :default-values    {}})

#_(def options
  {:grid-id           ::view
   :title             "Positions"
   :data-subscription [:positions]
   :id-field          :position/id
   :can-sort          true})

(defn view []
  (let [*schema (rf/subscribe [:model :model/position-dto])
        fields  (datagrid/model->fields
                 @*schema
                 {:fields-opts
                  {:instrument
                   {:menu [{:title "Edit"
                            :view  ::views.instruments/view}]}
                   :trade-pattern
                   {:menu [{:title "Edit"
                            :view  ::views.instruments/view}]}}})]
    [datagrid/view fields options]))
