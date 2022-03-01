(ns ra-datagrid.views.edit-cells
  (:require [ra-datagrid.views :as views :refer [edit-cell]]
            [re-frame.core :as rf]
            [reason-alpha.views.components.select2 :as select2]))

(defn options [name opts record-val]
  (for [{:keys [text value]} opts]
    ^{:key (str "opt-" name "-" value)}
    [:option (cond-> {:value (str value)}
               (= record-val value) (assoc :selected ""))
     text]))

(defmethod edit-cell :select
  [id field pk]
  (let [*r       (rf/subscribe [:datagrid/edited-record-by-pk id pk])
        *options (rf/subscribe (:data-subscription field))]
    (fn [id field pk]
      (let [{:keys [name]} field
            selected-val   (get @*r name)
            optgroups?     (-> @*options
                               first
                               :options
                               boolean)]
        (cljs.pprint/pprint {::edit-cell @*options})

        (select2/select2 @*options
                         (atom 2) nil
                         [:td {:key       name
                               :className "editing"}
                          [:select.list.form-control.select2-show-search {:style {:width            "100%"
                                                                                  :background-color "rgb(43, 57, 93)"}}]
                          #_(if optgroups?
                              [:select.form-control.select2-show-search
                               (for [{txt   :text
                                      gopts :options} @*options]
                                 ^{:key (str "optgroup-" name "-" txt)}
                                 [:optgroup {:label txt}
                                  [options name gopts v]])]
                              (into [:select.form-control.select2-show-search
                                     {:aria-hidden true}]
                                    (options name @*options v)))])))))

(comment
  ;; [:div.fg-line
  ;;  [:input.form-control {:type      "text"
  ;;                        :value     v
  ;;                        :on-change #(rf/dispatch [:datagrid/update-edited-record id pk
  ;;                                                  (:name field) (.-target.value %)])}]]

  )
