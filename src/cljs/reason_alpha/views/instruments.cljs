(ns reason-alpha.views.instruments
  (:require [reason-alpha.views.datagrid :as datagrid]
            [re-frame.core :as rf]))

#_(defn fields [providers-model]
  (let [{{ptitles :enum/titles} :properties
         providers              :members} providers-model

        symbol-fields (for [p    providers
                            :let [t (get ptitles p)]]
                        {:title    t
                         :name     p
                         :can-sort true})]
    (into
     [{:title    "Instrument"
       :name     :instrument-name
       :can-sort true}]
     cat
     [symbol-fields
      [{:title             "Type"
        :name              :instrument-type
        :type              :select
        :data-subscription [:models/members-of :model/instrument :instrument/type]}]])))

(def fields
  [{:title    "Instrument"
    :name     :instrument-name
    :can-sort true}
   {:title "Quantity"
    :name  :trade-transaction-quantity}
   {:title "Open Time"
    :name  :trade-transaction-date}
   {:title "Open"
    :name  :open-price}
   {:title "Close"
    :name  :close-price}
   {:title "Trade Pattern"
    :name  :trade-pattern-name}])

(def options
  {:grid-id           ::view
   :title             "Instruments"
   :data-subscription [:instruments]
   :id-field          :instrument/id
   :can-sort          true})

(defn view []
  (let [*symbol-providers (rf/subscribe [:models/members-of :model/symbol :symbol/provider])]
    [datagrid/view fields #_(fields @*symbol-providers) options]))
