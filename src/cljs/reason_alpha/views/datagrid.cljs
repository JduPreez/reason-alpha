(ns reason-alpha.views.datagrid
  (:require [ra-datagrid.views :as ra-datagrid]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reason-alpha.views :as views] ))

(def default-opts
  {;;:grid-id                    :my-grid
   ;;:data-subscription          [:large-data]
   :id-field                   :id
   ;;:header-filters             true
   ;;:progressive-loading        true
   ;;:can-sort                   true
   ;;:can-edit                   true
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

(def sheets (r/atom {}))

(defn view [fields options]
  (fn [fields {:keys [grid-id title] :as options}]
      (when-not (get sheets grid-id)
        (swap! sheets assoc grid-id options))
      [:div.card
       [:div.card-header.bg-gradient-indigo.br-tr-3.br-tl-3
        [:div.btn-list
         ^{:key grid-id}
         [:button.btn.btn-primary {:type "button"} title]
         (for [[sheet-grid-id {sheet-title :title}]
               , @sheets
               :when
               , (not= grid-id sheet-grid-id)]
           ^{:key sheet-grid-id}
           [:button.btn.btn-outline-primary
            {:on-click #(rf/dispatch (views/navigate sheet-grid-id))}
            sheet-title])]
        #_[:h2.card-title "Portfolio Trades"]]
       ;;[:div.card-status.bg-yellow.br-tr-3.br-tl-3]
       [:div.card-body {:style {:padding-top    0
                                :padding-bottom 0}}
        [ra-datagrid/datagrid (merge default-opts options) fields]]]))
