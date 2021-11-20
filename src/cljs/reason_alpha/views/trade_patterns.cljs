(ns reason-alpha.views.trade-patterns
  (:require [cljs-uuid-utils.core :as uuid]
            [clojure.string :as s]
            [re-frame.core :as rf]
            [reason-alpha.views.datagrid :as datagrid]
            [goog.string :as gstring]))

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
                                                             :on-change #(let [v (-> % .-target .-value)
                                                                               v (if (and (string? v)
                                                                                          (s/blank? v))
                                                                                   nil
                                                                                   v)
                                                                               v (if (uuid/valid-uuid? v)
                                                                                   (uuid/make-uuid-from v)
                                                                                   v)]
                                                                           (fn-dispatch v))}
                                       ^{:key (str "option-" id "-default-option")}
                                       [:option {:value ""} ""]
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
   :create-dispatch     [:trade-patterns/create]
   :update-dispatch     [:save :trade-patterns]})

(defn view []
  (fn []
    [datagrid/view fields options]))

