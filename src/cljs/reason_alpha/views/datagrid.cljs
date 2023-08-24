(ns reason-alpha.views.datagrid
  (:require [clojure.string :as str]
            [goog.string :as gstr]
            [goog.string.format]
            [malli.core :as m]
            [medley.core :as medley]
            [ra-datagrid.views :as ra-datagrid]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reason-alpha.model.utils :as mutils]
            [reason-alpha.views.utils :as vutils]
            [tick.core :as tick]
            [tick.locale-en-us]))

(def date-formatter (tick/formatter "yyyy-MM-dd"))

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
       {:style {:padding "0.5rem 0.5rem"}}
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
       [:div.card-body.card-body-datagrid 
        [ra-datagrid/datagrid (merge default-opts opts) fields]]])))

(defn model-member->field
  [[member-nm {:keys                         [type schema order]
               {o? :optional :as properties} :properties
               :as                           s}] &
   [{:keys [enum-titles] :as field-opts}]]
  (let [id-member?             (mutils/id-member? member-nm)
        is-number?             (or (= type (-> #'number? meta :name))
                                   (= type (-> #'float? meta :name)))
        field-def              (cond-> field-opts
                                 (not (contains? field-opts :can-sort))
                                 , (assoc :can-sort true)
                                 o?
                                 , (assoc :optional? o?)
                                 is-number?
                                 , (assoc :formatter
                                          #(when %
                                             (gstr/format "%.2f" %)))
                                 :default
                                 , (dissoc field-opts :ref-suffix))
        {:keys [title ref
                command-path]} properties
        field-def              (merge field-def {:title title
                                                 :name  member-nm})
        tuple-id-type          (when (= type :tuple)
                                 (second schema))
        data-subscr            (vutils/ref->data-sub ref)
        parent-subscr          (vutils/ref->parent-data-sub ref)]
    (cond
      ;; Id members are either the current entity's `:id` or `:creation-id` fields
      ;; or they should be 'foreign keys' with a `:ref` pointing to another entity
      ;; using an `<select>`
      (and id-member?
           (not ref))
      , nil

      (str/blank? title)
      , nil

      (nil? command-path)
      , (assoc field-def :type :no-edit)

      (and ref
           (not id-member?))
      , (merge
         (cond-> {:type              :select
                  :data-subscription data-subscr}
           (= tuple-id-type keyword?) (assoc :enum-titles enum-titles))
         field-def)

      (and ref
           id-member?)
      , (medley/deep-merge field-def
                           {:type              :indent-group
                            :data-subscription data-subscr
                            :indent-group
                            {:parent-subscription parent-subscr}})

      is-number?
      , (merge field-def {:type :number})

      (= type (-> #'inst? meta :name))
      , (merge field-def {:type  :date
                          :width 227})

      :default
      , field-def)))

(defn model->fields [model-schema & [{fields-opts :fields-opts}]]
  (->> model-schema
       mutils/model-member-schema-info
       (sort-by (fn [[k {:keys [order]}]] order))
       (mapv (fn [[membr-nm :as sch-inf]]
               (model-member->field sch-inf (get fields-opts membr-nm))))
       (remove nil?)))
