(ns reason-alpha.views.datagrid
  (:require [ra-datagrid.views :as ra-datagrid]
            [re-frame.core :as rf]
            [reagent.core :as r]
            ;;[reason-alpha.views :as views]
            ))

(def default-opts
  {;;:grid-id                    :my-grid
   ;;:data-subscription          [:large-data]
   :id-field                   :id
   ;;:header-filters             true
   ;;:progressive-loading        true
   ;;:can-sort                   true
   :can-edit                   true
   ;;:can-reorder                true
   :can-create                 true
   ;;:checkbox-select            true
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
    (let[*grid-options (rf/subscribe [:datagrid/options grid-id])]
      [:div.card
       [history-list grid-id title]
       [:div.card-body {:style {:padding-top    0
                                :padding-bottom 0}}
        [ra-datagrid/datagrid (merge default-opts options) fields]]])))
