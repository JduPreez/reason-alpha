(ns reason-alpha.views.trade-patterns
  (:require ["@ag-grid-community/react" :as ag-grd-react]
            ["@ag-grid-enterprise/all-modules" :as ag-grd]
            [reason-alpha.core :as core]
            [re-frame.core :as rf))

(defn view []
  (let [loading        @(rf/subscribe [:loading])
        trade-patterns @(rf/subscribe [:trade-patterns])]
    [:div.ag-theme-balham-dark {:style {:width  "100%"
                                        :height "100%"}}
     [:> ag-grd-react/AgGridReact
      {:columnDefs (:columns state)
       :rowData    (:data state)
       :modules    ag-grd/AllModules}]]))