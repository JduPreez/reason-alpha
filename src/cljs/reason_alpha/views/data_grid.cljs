(ns reason-alpha.views.data-grid
  (:require ["@ag-grid-community/react" :as ag-grd-react]
            ["@ag-grid-enterprise/all-modules" :as ag-grd]
            [goog.object]
            [medley.core :refer [assoc-some]]
            [re-frame.core :as rf]
            [reason-alpha.utils :as utils]))

(defn- get-value [*options params]
  (let [clj-params (js->clj params)
        field      (get-in clj-params ["colDef" "field"])
        item       (get clj-params "data")
        item-val   (get item field)]
    (if *options
      (some (fn [{:keys [label value]}]
              (when (= value item-val) label)) @*options)
      item-val)))

(defn- format-value [*options val]
  (some (fn [{:keys [label value]}]
          (when (= val value) label)) @*options))

(defn- is-editable? [select editable? field params]
  ;; If the column is a parent-child-select one, then make sure
  ;; editing is disabled for parent columns, to ensure the graph
  ;; is only 2 levels deep.
  (if (not select) editable?
      (let [clj-params (js->clj params)
            val        (get-in clj-params ["data" field])]
        (if val
          editable?
          false))))

(defn- columns [cols]
  (map (fn [[k {:keys [header
                       flex
                       min-width
                       max-width
                       editable?
                       select]}]]
         (let [field              (utils/keyword->str k)
               {:keys [lookup-key
                       *options]} select]
           (-> {:headerName   header
                :field        field
                :valueGetter  (partial get-value *options)
                :flex         flex
                :editable     (partial is-editable? select editable? field)}
               (assoc-some :minWidth min-width)
               (assoc-some :maxWidth max-width)
               (assoc-some :cellEditor
                           (when select
                             "agRichSelectCellEditor"))
               (assoc-some :cellEditorParams
                           (when  select
                             (fn [params]
                               (let [clj-params  (js->clj params)
                                     slookup-key (utils/keyword->str lookup-key)]
                                 (clj->js
                                  {:formatValue (partial format-value *options)
                                   :values      (->> @*options
                                                     (map :value)
                                                     (filter #(when (not= (get-in clj-params ["data" slookup-key]) %)
                                                                %)))}))))))))
       (seq cols)))

(defn- save [fn-save event]
  (let [data (-> event
                 (js->clj)
                 (get "data")
                 (utils/kw-keys))]
    (fn-save data)))

(defn view [{:keys [fn-save fn-get-id]} data cols & [tree-path fn-row-selected]]
  [:div.ag-theme-balham-dark {:style {#_:width #_ "100%"
                                      :height  "100%"}}
   [:> ag-grd-react/AgGridReact
    (cond-> {:defaultColDef                 {:resizable true}
             :rowSelection                  "single"
             :rowDeselection                true
             :immutableData                 true
             :columnDefs                    (columns cols)
             :rememberGroupStateWhenNewData true
             :rowData                       (utils/str-keys data)
             :modules                       ag-grd/AllModules
             :onFirstDataRendered           #(-> % .-api .sizeColumnsToFit)
             :onCellEditingStopped          (partial save fn-save)
             :getRowNodeId                  #(fn-get-id (-> %
                                                            js->clj
                                                            utils/kw-keys))
             :onSelectionChanged            (if fn-row-selected
                                              #(fn-row-selected (-> %
                                                                    .-api
                                                                    .getSelectedRows
                                                                    js->clj
                                                                    first
                                                                    utils/kw-keys))
                                              (fn [_]
                                                (js/console.log (str "Row selected for grid " type))))}
      tree-path (assoc :treeData true
                       :getDataPath #(goog.object/get
                                      %
                                      (utils/keyword->str tree-path))))]])
