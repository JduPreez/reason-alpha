(ns reason-alpha.views.positions
  (:require [re-frame.core :as rf]
            [reason-alpha.model.validation :as validation]
            [reason-alpha.utils :as utils]
            [reason-alpha.views.datagrid :as datagrid]
            [reason-alpha.views.holdings :as views.holdings]
            [reason-alpha.views.trade-patterns :as views.trade-patterns]))

(defn options
  [schema]
  {:grid-id              ::view
   :title                "Positions"
   :data-subscription    [:position/list]
   :id-field             :position-creation-id
   :create-dispatch      [:position/create]
   :update-dispatch      [:position/update]
   :validator            (partial validation/validate schema)
   :default-values       {:position-creation-id (constantly (utils/new-uuid))}
   :context-subscription [:account]})

(defn view [& x]
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
    [datagrid/view fields (options @*schema)]))
