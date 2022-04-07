(ns reason-alpha.views.holdings
  (:require [reason-alpha.views.datagrid :as datagrid]
            [re-frame.core :as rf]))

(def options
  {:grid-id           ::view
   :title             "Holdings (Instruments)"
   :data-subscription [:holding/list]
   :id-field          :holding-creation-id
   :create-dispatch   [:holding.command/create]
   :update-dispatch   [:holding.command/update]
   :default-values    {:instrument-type [:share ""]}})

(defn view []
  (let [*schema      (rf/subscribe [:model :model/holding-dto])
        *type-titles (rf/subscribe [:holding/instrument-type-titles])
        fields       (datagrid/model->fields
                      @*schema
                      {:fields-opts
                       {:instrument-type
                        {:enum-titles @*type-titles}}})]
    [datagrid/view fields options]))
