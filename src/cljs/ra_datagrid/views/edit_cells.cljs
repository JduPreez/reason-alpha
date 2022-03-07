(ns ra-datagrid.views.edit-cells
  (:require [ra-datagrid.views :as views :refer [edit-cell]]
            [re-frame.core :as rf]
            [reason-alpha.views.components.select2 :as select2]
            [reagent.core :as reagent]))

(defn options [name opts record-val]
  (for [{:keys [text value]} opts]
    ^{:key (str "opt-" name "-" value)}
    [:option (cond-> {:value (str value)}
               (= record-val value) (assoc :selected ""))
     text]))

(defmethod edit-cell :select
  [id field pk]
  (let [*r            (rf/subscribe [:datagrid/edited-record-by-pk id pk])
        *options      (rf/subscribe (:data-subscription field))
        *selected-val (reagent/atom (get @*r (:name field)))]
    (fn [id field pk]
      (let [#_#_              {:keys [name]} field
            #_#_*selected-val (atom (get @*r name))
            optgroups?        (-> @*options
                                  first
                                  :options
                                  boolean)]
        (cljs.pprint/pprint {::edit-cell @*options})

        (select2/select2 @*options
                         *selected-val nil
                         [:td {:key       name
                               :className "editing"}
                          [:select.list.form-control.select2-show-search
                           {:on-change #(js/log (.-target.value ^js %)) #_#(rf/dispatch [:datagrid/update-edited-record id pk
                                                                                         (:name field) (.-target.value ^js %)])}]])))))

(comment
  ;; [:div.fg-line
  ;;  [:input.form-control {:type      "text"
  ;;                        :value     v
  ;;                        :on-change #(rf/dispatch [:datagrid/update-edited-record id pk
  ;;                                                  (:name field) (.-target.value %)])}]]

  )
