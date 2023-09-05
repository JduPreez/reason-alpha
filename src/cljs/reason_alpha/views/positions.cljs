(ns reason-alpha.views.positions
  (:require [re-frame.core :as rf]
            [reason-alpha.model.validation :as validation]
            [reason-alpha.utils :as utils]
            [reason-alpha.views.datagrid :as datagrid]
            [reason-alpha.views.holdings :as views.holdings]
            [reason-alpha.views.trade-patterns :as views.trade-patterns]
            [tick.core :as tick]))

(defn options
  [schema]
  {:grid-id                ::view
   :title                  "Positions"
   :data-subscription      [:position/list]
   :id-field               :position-creation-id
   :default-sort-key       :holding
   :default-sort-direction :asc
   :create-dispatch        [:position/create]
   :update-dispatch        [:position/update]
   :validator              (partial validation/validate schema)
   :default-values         [:position/default-vals]
   :context-subscription   [:account]
   :can-edit-fn            #(nil? (seq (:sub-positions %)))})

(defn view [& x]
  (let [*schema    (rf/subscribe [:model :model/position-dto])
        *ls-titles (rf/subscribe [:position/long-short-titles])
        fields     (datagrid/model->fields
                    @*schema
                    {:fields-opts
                     {:holding
                      {:sort-value-fn #(second %)
                       :menu          [{:title "Edit"
                                        :view  ::views.holdings/view}]}
                      :trade-pattern
                      {:menu [{:title "Edit"
                               :view  ::views.trade-patterns/view}]}

                      :long-short
                      {:enum-titles @*ls-titles}

                      :holding-position-id
                      {:width        "180px"
                       :indent-group {:group-path   [:position-id]
                                      ;; holding -> then 2nd child of tuple, which is the
                                      ;; holding name
                                      :display-name (fn hoilding-pos-display-nm
                                                      [{:keys [open-date holding]}]
                                                      (when (and open-date holding)
                                                        (let [[_ h]    holding
                                                              open-dte (->> open-date
                                                                            tick/date
                                                                            (tick/format datagrid/date-formatter))]
                                                          (str h " (" open-dte ")"))))}}}})]
    [datagrid/view fields (options @*schema)]))
