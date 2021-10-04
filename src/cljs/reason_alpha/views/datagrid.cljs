(ns reason-alpha.views.datagrid
  (:require [medley.core :refer [assoc-some]]
            [ra-datagrid.views :as ra-datagrid]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(def default-opts
  {;;:grid-id                    :my-grid
   ;;:data-subscription          [:large-data]
   :id-field                   :id
   ;;:header-filters             true
   ;;:progressive-loading        true
   ;;:can-sort                   true
   ;;:can-edit                   true
   ;;:can-reorder                true
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
   ;;:update-dispatch            [:update]
   ;;:delete-dispatch            [:delete]
   :additional-css-class-names "table-striped table-sm"})

(defn view [fields options]
  [ra-datagrid/datagrid (merge default-opts options) fields])
