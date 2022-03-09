(ns ra-datagrid.views.fields
  (:require [ra-datagrid.views :as views :refer [edit-cell]]
            [ra-datagrid.subs :as subs :refer [default-formatter]]
            [re-com.dropdown :as dropdown]
            [re-com.core :as re-com]
            [re-frame.core :as rf]
            [reagent.core :as reagent]))

(defmethod default-formatter :select
  [{enum-titles :enum-titles}]
  (fn [v _]
    (if (keyword? v)
      (get enum-titles v)
      (str v))))

(defmethod edit-cell :select
  [id field pk]
  (let [*r            (rf/subscribe [:datagrid/edited-record-by-pk id pk])
        *choices      (rf/subscribe (:data-subscription field))
        *selected-val (reagent/atom (name (get @*r (:name field))))]
    (fn [id field pk]
      [:td {:key       (:name field)
            :className "editing"}
         [dropdown/single-dropdown
          :src         (re-com/at)
          :choices     @*choices ;;grouped-countries
          :model       *selected-val
          :width       "300px"
          :max-height  "200px"
          :class       "form-control"
          :style       {:padding "0"}
          :filter-box? true
          :on-change   #(do
                          (reset! *selected-val %)
                          (rf/dispatch [:datagrid/update-edited-record id pk
                                        (:name field) (keyword @*selected-val)]))]])))

(comment
  ;; [:div.fg-line
  ;;  [:input.form-control {:type      "text"
  ;;                        :value     v
                         ;; :on-change #(rf/dispatch [:datagrid/update-edited-record id pk
                         ;;                           (:name field) (.-target.value %)])}]]

  )
