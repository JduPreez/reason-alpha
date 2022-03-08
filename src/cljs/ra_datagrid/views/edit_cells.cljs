(ns ra-datagrid.views.edit-cells
  (:require [ra-datagrid.views :as views :refer [edit-cell]]
            [re-com.dropdown :as dropdown]
            [re-com.core :as re-com]
            [re-frame.core :as rf]
            [reagent.core :as reagent]))

(def grouped-countries [{:id "AU" :label "Australia" :group "EN Speakers"}
                        {:id "US" :label "United States" :group "EN Speakers"}
                        {:id "GB" :label "United Kingdom" :group "EN Speakers"}
                        {:id "E1" :label "Iraq" :group "Updated Axis Of Evil"}
                        {:id "E2" :label "New Zealand" :group "Updated Axis Of Evil"}
                        {:id "E3" :label "Iran" :group "Updated Axis Of Evil"}
                        {:id "E4" :label "North Korea" :group "Updated Axis Of Evil"}
                        {:id "03" :label "Afghanistan" :group "'A' COUNTRIES"}
                        {:id "04" :label "Albania" :group "'A' COUNTRIES"}
                        {:id "05" :label "Algeria" :group "'A' COUNTRIES"}
                        {:id "06" :label "American Samoa" :group "'A' COUNTRIES"}
                        {:id "07" :label "Andorra" :group "'A' COUNTRIES"}
                        {:id "08" :label "Angola" :group "'A' COUNTRIES"}
                        {:id "09" :label "Anguilla" :group "'A' COUNTRIES"}
                        {:id "10" :label "Antarctica" :group "'A' COUNTRIES"}
                        {:id "11" :label "Antigua and Barbuda" :group "'A' COUNTRIES"}
                        {:id "12" :label "Argentina" :group "'A' COUNTRIES"}
                        {:id "13" :label "Armenia" :group "'A' COUNTRIES"}
                        {:id "14" :label "Aruba" :group "'A' COUNTRIES"}
                        {:id "16" :label "Austria" :group "'A' COUNTRIES"}
                        {:id "17" :label "Azerbaijan" :group "'A' COUNTRIES"}
                        {:id "18" :label "Bahamas" :group "'B' COUNTRIES"}
                        {:id "19" :label "Bahrain" :group "'B' COUNTRIES"}
                        {:id "20" :label "Bangladesh" :group "'B' COUNTRIES"}
                        {:id "21" :label "Barbados" :group "'B' COUNTRIES"}
                        {:id "22" :label "Belarus" :group "'B' COUNTRIES"}
                        {:id "23" :label "Belgium" :group "'B' COUNTRIES"}
                        {:id "24" :label "Belize" :group "'B' COUNTRIES"}
                        {:id "25" :label "Benin" :group "'B' COUNTRIES"}
                        {:id "26" :label "Bermuda" :group "'B' COUNTRIES"}
                        {:id "27" :label "Bhutan" :group "'B' COUNTRIES"}
                        {:id "28" :label "Bolivia" :group "'B' COUNTRIES"}
                        {:id "29" :label "Bosnia and Herzegovina" :group "'B' COUNTRIES"}
                        {:id "30" :label "Botswana" :group "'B' COUNTRIES"}
                        {:id "31" :label "Bouvet Island" :group "'B' COUNTRIES"}
                        {:id "32" :label "Brazil" :group "'B' COUNTRIES"}
                        {:id "34" :label "Brunei Darussalam" :group "'B' COUNTRIES"}
                        {:id "35" :label "Bulgaria" :group "'B' COUNTRIES"}
                        {:id "36" :label "Burkina Faso" :group "'B' COUNTRIES"}
                        {:id "37" :label "Burundi" :group "'B' COUNTRIES"}])

(defmethod edit-cell :select
  [id field pk]
  (let [*r            (rf/subscribe [:datagrid/edited-record-by-pk id pk])
        *choices      (rf/subscribe (:data-subscription field))
        ;;*selected-val (reagent/atom (get @*r (:name field)))
        *selected-val (reagent/atom "US")]
    (fn [id field pk]
      (let [#_#_              {:keys [name]} field
            #_#_*selected-val (atom (get @*r name))
            ]
        (cljs.pprint/pprint {::edit-cell @*choices})
        [:td {:key       (:name field)
              :className "editing"}
         [dropdown/single-dropdown
          :src         (re-com/at)
          :choices     grouped-countries
          :model       *selected-val
          :width       "300px"
          :max-height  "200px"
          :class       "form-control"
          :style       {:padding "0"}
          :filter-box? true
          :on-change   #(reset! *selected-val %)]]))))

(comment
  ;; [:div.fg-line
  ;;  [:input.form-control {:type      "text"
  ;;                        :value     v
  ;;                        :on-change #(rf/dispatch [:datagrid/update-edited-record id pk
  ;;                                                  (:name field) (.-target.value %)])}]]

  )
