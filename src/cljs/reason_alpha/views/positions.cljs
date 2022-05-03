(ns reason-alpha.views.positions
  (:require [re-frame.core :as rf]
            [reason-alpha.views.datagrid :as datagrid]
            [reason-alpha.views.holdings :as views.holdings]
            [reason-alpha.views.trade-patterns :as views.trade-patterns]))

(def options
  {:grid-id           ::view
   :title             "Positions"
   :data-subscription [:position/list]
   :id-field          :position-creation-id
   :create-dispatch   [:position/create]
   :update-dispatch   [:position/update]
   :default-values    {}})

(defn view []
  (let [*schema    (rf/subscribe [:model :model/position-dto])
        *ls-titles (rf/subscribe [:position/long-short-titles])
        fields     (datagrid/model->fields
                    @*schema
                    {:fields-opts
                     {:holding
                      {:menu [{:title "Edit"
                               :view  ::views.holdings/view}]}

                      :trade-pattern
                      {:menu [{:title "Edit"
                               :view  ::views.trade-patterns/view}]}

                      :long-short
                      {:enum-titles @*ls-titles}

                      :holding-position-id
                      {:indent-group {:group-path        [:position-id]
                                      :display-name-path [:holding 1]}}}})]
    [datagrid/view fields options]))
