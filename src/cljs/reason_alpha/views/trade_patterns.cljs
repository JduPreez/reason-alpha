(ns reason-alpha.views.trade-patterns
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [reason-alpha.utils :as utils]
            [reason-alpha.views.datagrid :as datagrid]))

(def fields
  [{:title    "Trade Pattern"
    :name     :trade-pattern-name
    :can-sort true}
   {:title    "Description"
    :name     :trade-pattern-description
    :can-sort true}
   {:title                        "Parent"
    :name                         :trade-pattern-parent-id
    :type                         :custom
    :custom-element-renderer      (fn [{parent-id :trade-pattern-parent-id
                                        :as       _record}]
                                    (let [{name :trade-pattern-name} @(rf/subscribe
                                                                       [:trade-pattern parent-id])]
                                      name))
    :custom-element-edit-renderer (fn [_field {id        :trade-pattern-id
                                               parent-id :trade-pattern-parent-id
                                               :as       _record} fn-dispatch _val]
                                    (let [tps              @(rf/subscribe [:trade-pattern/list])
                                          eligible-parents (->> tps
                                                                (remove #(or (nil? (:trade-pattern-id %)) ;; If not saved to back-end
                                                                             (= (:trade-pattern-id %) id) ;; If option is this record
                                                                             (:trade-pattern-parent-id %) ;; If option is a child
                                                                             (some (fn [tp] ;; If record has a child somewhere
                                                                                     (and id
                                                                                          (= (:trade-pattern-parent-id tp) id)))
                                                                                   tps)))
                                                                (sort-by :trade-pattern-name))]
                                      (cljs.pprint/pprint {::edit-render {:T  tps
                                                                          :EP eligible-parents}})
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
                                       (for [{ep-id   :trade-pattern-id
                                              ep-name :trade-pattern-name} eligible-parents]
                                         ^{:key (str "option-" id "-" ep-id)}
                                         [:option {:value ep-id} ep-name])]))}])

(def options
  {:grid-id             ::view
   :title               "Trade Patterns"
   :data-subscription   [:trade-pattern/list]
   :id-field            :trade-pattern-creation-id
   :group-by            {:group-key  :trade-pattern-id
                         :member-key :trade-pattern-parent-id}
   :checkbox-select     true
   :on-selection-change #(rf/dispatch [:select %1])
   :create-dispatch     [:trade-pattern.command/create]
   :update-dispatch     [:trade-pattern.command/update]})

(defn view []
  (fn []
    [datagrid/view fields options]))

