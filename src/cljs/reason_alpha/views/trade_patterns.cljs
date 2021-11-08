(ns reason-alpha.views.trade-patterns
  (:require [reason-alpha.views.datagrid :as datagrid]
            [re-frame.core :as rf]))

(def model :trade-patterns)

(def fields
  [{:title    "Trade Pattern"
    :name     :trade-pattern/name
    :can-sort true}
   {:title    "Description"
    :name     :trade-pattern/description
    :can-sort true}])

(def options
  {:grid-id             :trade-patterns
   :title               "Trade Patterns"
   :data-subscription   [:trade-patterns]
   :id-field            :trade-pattern/id
   :can-sort            true
   :can-edit            true
   :group-by            :trade-pattern/parent-id
   :checkbox-select     true
   :on-selection-change #(rf/dispatch [:select %1])})

(defn view []
  (fn []
    [datagrid/view fields options]
    #_(data-grid/view {:fn-save #(rf/dispatch [:save :trade-patterns %])
                       :fn-get-id (fn [{:keys [trade-pattern/id
                                               trade-pattern/creation-id]
                                        :as   _data}]
                                    (or id creation-id))}
                    @(rf/subscribe [:trade-patterns])
                    {:trade-pattern/name        {:header    "Trade Pattern"
                                                 :flex      1
                                                 :min-width 200
                                                 :max-width 230
                                                 :editable? true}
                     :trade-pattern/parent-id   {:header    "Parent"
                                                 :flex      1
                                                 :min-width 200
                                                 :max-width 250
                                                 :select    {:lookup-key :trade-pattern/id
                                                             :*ref-data  (rf/subscribe [:trade-patterns/ref-data])
                                                             :*options   (rf/subscribe [:trade-pattern-options])}
                                                 :editable? true}
                     :trade-pattern/description {:header    "Description"
                                                 :flex      2
                                                 :editable? true}}
                    :trade-pattern/ancestors-path
                    #(rf/dispatch [:select %]))))

