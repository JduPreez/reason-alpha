(ns reason-alpha.views.trade-patterns
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [reason-alpha.utils :as utils]
            [reason-alpha.views.datagrid :as datagrid]))

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
                                                                             (:trade-pattern/parent-id %)
                                                                             (some (fn [tp]
                                                                                     (= (:trade-pattern/parent-id tp) id))
                                                                                   @*tps)))
                                                                (sort-by :trade-pattern/name))]
                                      [:select.form-control {:value     (or parent-id "")
                                                             :on-change #(let [v (as-> % v
                                                                                   (.-target v)
                                                                                   (.-value v)
                                                                                   (if (and (string? v)
                                                                                            (str/blank? v))
                                                                                     nil
                                                                                     v)
                                                                                   (utils/maybe->uuid v))]
                                                                           (fn-dispatch v))}
                                       ^{:key (str "option-" id "-default-option")}
                                       [:option {:value ""} ""]
                                       (for [{ep-id   :trade-pattern/id
                                              ep-name :trade-pattern/name} eligible-parents]
                                         ^{:key (str "option-" id "-" ep-id)}
                                         [:option {:value ep-id} ep-name])]))}])

(comment
  (as-> {:data 12345} x
    (if (= (:data x) 12345)
       true
       false))
  
  )

(def options
  {:grid-id             :trade-patterns
   :title               "Trade Patterns"
   :data-subscription   [:trade-patterns]
   :id-field            :trade-pattern/creation-id
   :can-sort            true
   :can-edit            true
   :group-by            {:group-key  :trade-pattern/id
                         :member-key :trade-pattern/parent-id} ;;:trade-pattern/parent-id
   :checkbox-select     true
   :on-selection-change #(rf/dispatch [:select %1])
   :create-dispatch     [:trade-patterns/create]
   :update-dispatch     [:save :trade-patterns]})

(defn view []
  (fn []
    [datagrid/view fields options]))

