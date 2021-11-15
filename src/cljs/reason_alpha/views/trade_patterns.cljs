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
    :can-sort true}
   {:title                        "Parent"
    :name                         :trade-pattern/parent-id
    :type                         :custom
    :custom-element-renderer      (fn [{:keys [trade-pattern/parent-id] :as _record}]
                                    (let [{:keys [trade-pattern/name]} @(rf/subscribe
                                                                         [:trade-pattern parent-id])]
                                      name))
    :custom-element-edit-renderer (fn [_field {:keys [trade-pattern/id trade-pattern/parent-id]
                                               :as   _record} fn-dispatch _val]
                                    (let [*tps             (rf/subscribe [:trade-patterns])
                                          eligible-parents (->> @*tps
                                                                (remove #(or (= (:trade-pattern/id %) id)
                                                                             (:trade-pattern/parent-id %)))
                                                                (sort-by :trade-pattern/name))]
                                      [:select.form-control (cond-> {:on-change #(fn-dispatch
                                                                                  (-> % .-target .-value ))}
                                                              parent-id (assoc :value parent-id))
                                       ^{:key "default-option"}
                                       [:option]
                                       (for [{ep-id   :trade-pattern/id
                                              ep-name :trade-pattern/name} eligible-parents]
                                         ^{:key (str "option-" id "-" ep-id)}
                                         [:option {:value ep-id} ep-name])]))}])

(def options
  {:grid-id             :trade-patterns
   :title               "Trade Patterns"
   :data-subscription   [:trade-patterns]
   :id-field            :trade-pattern/id
   :can-sort            true
   :can-edit            true
   :group-by            :trade-pattern/parent-id
   :checkbox-select     true
   :on-selection-change #(rf/dispatch [:select %1])
   :create-dispatch     [:trade-patterns/create]})

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

