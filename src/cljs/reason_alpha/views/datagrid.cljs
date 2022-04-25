(ns reason-alpha.views.datagrid
  (:require [clojure.string :as str]
            [medley.core :as medley]
            [ra-datagrid.views :as ra-datagrid]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(def default-opts
  {;;:grid-id                    :my-grid
   ;;:data-subscription          [:large-data]
   :id-field                   :id
   ;;:header-filters             true
   ;;:progressive-loading        true
   :can-sort                   false
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
  [[member-nm & schema] & [{:keys [enum-titles] :as field-opts}]]
  (let [ref-suffix           "ref"
        ref-suffix-list      (str ref-suffix "-list")
        id-member            (-> member-nm
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
                                        [%])) schema)
        data-subscr          (if ref-ns
                               (keyword ref-ns (str ref-nm "-" ref-suffix-list))
                               (keyword ref-nm ref-suffix-list))
        parent-subscr        (if ref-ns
                               (keyword ref-ns (str ref-nm "-" ref-suffix))
                               (keyword ref-nm ref-suffix))]
    (cond
      ;; Id members are either the current entity's `:id` or `:creation-id` fields
      ;; or they should be 'foreign keys' with a `:ref` pointing to another entity
      ;; using an `<select>`
      (and id-member
           (not ref))
      , nil

      (str/blank? title)
      , nil

      #_#_ (and ref-ns
                ref)
      , (merge
         (cond-> {:type              :select
                  :data-subscription [(keyword ref-ns (str ref-nm "-" ref-suffix))]}
           (= tuple-id-type keyword?) (assoc :enum-titles enum-titles))
         field-def)

      (and ref
           (not id-member))
      , (merge
         (cond-> {:type              :select
                  :data-subscription [data-subscr]}
           (= tuple-id-type keyword?) (assoc :enum-titles enum-titles))
         field-def)

      (and ref
           id-member)
      , (medley/deep-merge field-def
                           {:type              :indent-group
                            :data-subscription [data-subscr]
                            :indent-group
                            {:parent-subscription parent-subscr}})

      (= type (-> #'float? meta :name))
      , (merge field-def {:type :number})

      (= type (-> #'inst? meta :name))
      , (merge field-def {:type  :date
                          :width 227})

      :default
      , field-def)))

(defn model->fields [[_ & members] & [{fields-opts :fields-opts}]]
  (->> members
       (mapv (fn [[membr-k & _ :as m]]
               (model-member->field m (get fields-opts membr-k))))
       (remove nil?)))
