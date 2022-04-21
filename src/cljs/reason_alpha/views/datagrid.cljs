(ns reason-alpha.views.datagrid
  (:require [ra-datagrid.views :as ra-datagrid]
            [re-frame.core :as rf]
            [reagent.core :as r]
            ;;[reason-alpha.views :as views]

            [clojure.string :as str]))

(def default-opts
  {;;:grid-id                    :my-grid
   ;;:data-subscription          [:large-data]
   :id-field                   :id
   ;;:header-filters             true
   ;;:progressive-loading        true
   :can-sort                   true
   :can-edit                   true
   ;;:can-reorder                true
   :can-create                 true
   :checkbox-select            true
   :on-selection-change        #(rf/dispatch [:select %1])
   ;;:loading-subscription       [:my-loading]
   ;;:sort-dispatch              [:sort]
   ;;:header-filter-dispatch     [:header-filter-dispatch]
   ;;:reorder-dispatch           [:reorder]
   #_#_:extra-header-row       [:tr
                                [:th
                                 {:col-span 6}
                                 "Extra custom header row"]]
   ;;:create-dispatch            [:create]
   ;;:start-edit-dispatch        [:start-edit]
   ;;:update-dispatch            [:save]
   ;;:delete-dispatch            [:delete]
   :additional-css-class-names "table-striped table-sm"})

(defn history-item [grid-id]
  (fn [grid-id]
    (let [title @(rf/subscribe
                  [:datagrid/title grid-id])]
      [:button.btn.btn-outline-primary
       {:on-click #(rf/dispatch [:push-state grid-id])}
       title])))

(defn history-list [grid-id title]
  (fn [grid-id title]
    (let [history (or (and grid-id
                           @(rf/subscribe [:datagrid/history grid-id]))
                      [])]
      [:div.card-header.bg-gradient-indigo.br-tr-3.br-tl-3
       [:div.btn-list
        (for [hgrid-id history]
          (if (= hgrid-id grid-id)
            ^{:key (str "history-item-" hgrid-id)}
            [:button.btn.btn-primary {:type "button"} title]
            ^{:key (str "history-item-" hgrid-id)}
            [history-item hgrid-id]))]])))

(defn view [fields {:keys [grid-id] :as options}]
  (fn [fields {:keys [grid-id title] :as options}]
    (let[*options (rf/subscribe [:datagrid/options grid-id])
         opts     (or options @*options)]
      [:div.card
       [history-list grid-id title]
       [:div.card-body {:style {:padding-top    0
                                :padding-bottom 0}}
        [ra-datagrid/datagrid (merge default-opts opts) fields]]])))

(defn model-member->field
  [[member-nm & schema] & [{:keys [ref-suffix enum-titles]
                            :as   field-opts}]]
  (let [id-member            (-> member-nm
                                 name
                                 (str/ends-with? "-id"))
        field-def            (cond-> field-opts
                               (not (contains? field-opts :can-sort))
                               , (assoc :can-sort true)
                               :default
                               , (dissoc field-opts :ref-suffix))
        props-or-type        (first schema)
        has-props?           (map? props-or-type)
        {:keys [title ref]}  props-or-type
        field-def            (merge field-def {:title title
                                               :name  member-nm})
        ref-nm               (when ref
                               (name ref))
        ref-ns               (when ref
                               (namespace ref))
        [type tuple-id-type] (some #(when (not (map? %))
                                      (if (sequential? %)
                                        %
                                        [%])) schema)]
    (cond
      ;; Id members are either the current entity's `:id` or `:creation-id` fields
      ;; or they should be 'foreign keys' with a `:ref` pointing to another entity
      ;; using an `<select>`
      (and id-member
           (not ref))
      , nil

      (str/blank? title)
      , nil

      (and ref-ns
           ref)
      , (merge
         (cond-> {:type              :select
                  :data-subscription [(keyword ref-ns (str ref-nm "-" ref-suffix))]}
           (= tuple-id-type keyword?) (assoc :enum-titles enum-titles))
         field-def)

      ref
      , (merge
         (cond-> {:type              :select
                  :data-subscription [(keyword ref-nm ref-suffix)]}
           (= tuple-id-type keyword?) (assoc :enum-titles enum-titles))
         field-def)

      (= type (-> #'float? meta :name))
      , (merge field-def {:type :number})

      (= type (-> #'inst? meta :name))
      , (merge field-def {:type  :date
                          :width 227})

      :default
      , field-def)))

(defn model->fields [[_ & members] & [{fields-opts :fields-opts
                                       ref-suffix  :ref-suffix
                                       :or         {ref-suffix "ref-list"}}]]
  (->> members
       (mapv (fn [[membr-k & _ :as m]]
               (let [fo (-> fields-opts
                            (get membr-k)
                            (assoc :ref-suffix "ref-list"))]
                 (model-member->field m fo))))
       (remove nil?)))
