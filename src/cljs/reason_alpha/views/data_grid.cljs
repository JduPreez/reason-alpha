(ns reason-alpha.views.data-grid
  (:require ["@ag-grid-community/react" :as ag-grd-react]
            ["@ag-grid-enterprise/all-modules" :as ag-grd]
            [medley.core :refer [assoc-some]]
            [reason-alpha.utils :as utils]
            [re-frame.core :as rf]
            [goog.object]))

(defn- value [params]
  (let [clj-params (js->clj params)
        field      (get-in clj-params ["colDef" "field"])
        item       (get clj-params "data")] 
    (get item field)))

(defn- columns [cols]
  (map (fn [[k {:keys [header
                       flex
                       min-width
                       max-width
                       editable]}]]
         (-> {:headerName header
              :field              (utils/keyword->str k)
              :valueGetter        value
              :flex               flex}
             (assoc-some :minWidth min-width)
             (assoc-some :maxWidth max-width)
             (assoc-some :editable editable))) (seq cols)))

(defn- save [type event]
  (let [data (-> event
                 (js->clj)
                 (get "data")
                 (utils/kw-keys))]
    (rf/dispatch [:save type data])))

(defn view [type data cols & [tree-path]]
  (js/console.log "data-grid/view " (pr-str data))
  [:div.ag-theme-balham-dark {:style {#_:width #_ "100%"
                                      :height  "100%"}}
   [:> ag-grd-react/AgGridReact
    (cond-> {:defaultColDef        {:resizable true}
             :columnDefs           (columns cols)
             :rowData              data
             :modules              ag-grd/AllModules
             :onFirstDataRendered  #(-> % .-api .sizeColumnsToFit)
             :onCellEditingStopped (partial save type)}
      tree-path (assoc :getDataPath #(goog.object/get
                                      %
                                      (utils/keyword->str tree-path))))]])

(comment
  .-api .sizeColumnsToFit %
  (columns {:trade-pattern/name        {:header "Name"}
            :trade-pattern/description {:header "Description"}}))
