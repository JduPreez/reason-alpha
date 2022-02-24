(ns ra-datagrid.views.edit-cells
  (:require [ra-datagrid.views :as views :refer [edit-cell]]
            [re-frame.core :as rf]))

(defn options [name opts record-val]
  (for [{:keys [text value]} opts]
    ^{:key (str "opt-" name "-" value)}
    [:option (cond-> {:value value}
               (= record-val value) (assoc :selected ""))
     text]))

(defmethod edit-cell :select
  [id field pk]
  (let [*r       (rf/subscribe [:datagrid/edited-record-by-pk id pk])
        *options (rf/subscribe (:data-subscription field))]
    (fn [id field pk]
      (let [{:keys [name]} field
            v              (get @*r name)
            optgroups?     (-> @*options
                               first
                               :options
                               boolean)]
        [:td {:key       name
              :className "editing"}
         [:select.form-control.select2-show-search.select2-hidden-accessible
          {:aria-hidden true}
          [:select
           (if optgroups?
             (for [{:keys [text options]} @*options]
               ^{:key (str "optgroup-" name "-" text)}
               [:optgroup {:label text}
                 [options name options v]])
             [options name @*options v])]]]))))

(comment
  ;; [:div.fg-line
  ;;  [:input.form-control {:type      "text"
  ;;                        :value     v
  ;;                        :on-change #(rf/dispatch [:datagrid/update-edited-record id pk
  ;;                                                  (:name field) (.-target.value %)])}]]

  )
