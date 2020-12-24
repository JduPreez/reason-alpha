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

(defn- columns [cols]
  (map (fn [[k {:keys [header
                       flex
                       min-width
                       max-width
                       editable
                       select]}]]
         (let [field              (utils/keyword->str k)
               {:keys [lookup-key
                       *options]} select]
           (-> {:headerName  header
                :field       field
                :valueGetter (partial get-value *options)
                :flex        flex}
               (assoc-some :minWidth min-width)
               (assoc-some :maxWidth max-width)
               (assoc-some :editable (fn [params]
                                       ;; If the column is a parent-child-select one, then make sure
                                       ;; editing is disabled for parent columns, to ensure the graph
                                       ;; is only 2 levels deep.
                                       (if (not select) editable
                                           (let [clj-params (js->clj params)
                                                 val        (get-in clj-params ["data" field])]
                                             (if val
                                               editable
                                               false)))))
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

(comment
  (let [{:keys [lookup-key
                values]} select
        slookup-key      (utils/keyword->str lookup-key)]
    (fn [params]
      (let [clj-params (js->clj params)]
        {:values        (->> values
                             (map :value)
                             (filter #(when (not= (get-in clj-params ["data" slookup-key]) %)
                                        (cljs.pprint/pprint {::columns {:x (get-in clj-params ["data" slookup-key])
                                                                        :y %}})
                                        %)))
         #_:formatValue #_ (partial format-value select)})))
  )

(defn- save [type event]
  (let [data (-> event
                 (js->clj)
                 (get "data")
                 (utils/kw-keys))]
    (rf/dispatch [:save type data])))

(defn view [type data cols & [tree-path]]
  (cljs.pprint/pprint {::view (utils/str-keys data)})
  [:div.ag-theme-balham-dark {:style {#_:width #_ "100%"
                                      :height  "100%"}}
   [:> ag-grd-react/AgGridReact
    (cond-> {:defaultColDef        {:resizable true}
             :columnDefs           (columns cols)
             :rowData              (utils/str-keys data)
             :modules              ag-grd/AllModules
             :onFirstDataRendered  #(-> % .-api .sizeColumnsToFit)
             :onCellEditingStopped (partial save type)}
      tree-path (assoc :treeData true
                       :getDataPath #(goog.object/get
                                      %
                                      (utils/keyword->str tree-path))))]])

(comment
  (cond-> {:prop1 1}
    :trade-pattern/ancestors-path (assoc :getPathData 2))

  .-api .sizeColumnsToFit %
  (columns {:trade-pattern/name        {:header "Name"}
            :trade-pattern/description {:header "Description"}}))
