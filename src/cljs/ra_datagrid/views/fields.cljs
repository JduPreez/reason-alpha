(ns ra-datagrid.views.fields
  (:require [ra-datagrid.subs :as subs :refer [default-formatter]]
            [ra-datagrid.views :as views :refer [edit-cell]]
            [re-com.core :as re-com]
            [re-com.dropdown :as dropdown]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reason-alpha.utils :as utils]
            [medley.core :as medley]))

(defmethod default-formatter :select
  [{enum-titles :enum-titles}]
  (fn [v _]
    (cljs.pprint/pprint {::default-formatter _})
    (if (keyword? v)
      (get enum-titles v)
      (str v))))

(defn choice-id [choices]
  (-> choices first :id))

(defn choice-type [v]
  (cond
    (keyword? v)
    , :keyword

    (medley/uuid? v)
    , :uuid

    (medley/boolean? v)
    , :bool

    :else :string))

(defn val->choice-id [vtype v]
  (if (= vtype :keyword)
    (utils/keyword->str v)
    (str v)))

(defn choice-id->val [vtype cid]
  (case vtype
    :keyword (keyword cid)
    :uuid    (medley/uuid cid)
    :bool    (utils/str->bool cid)
    cid))

(defmethod edit-cell :select
  [id field pk]
  (let [*r            (rf/subscribe [:datagrid/edited-record-by-pk id pk])
        *choices      (rf/subscribe (:data-subscription field))
        _             (cljs.pprint/pprint {::edit-cell @*choices})
        cid-type      (-> @*choices
                          choice-type)
        *selected-val (reagent/atom (->> field
                                         :name
                                         (get @*r)
                                         (val->choice-id cid-type)))]
    (fn [id field pk]
      (let [cid-type (-> @*choices
                         choice-type)
            choices  (map (fn [{:keys [id] :as c}]
                            (update
                             c :id
                             #(val->choice-id cid-type %)))
                          @*choices)]
        [:td {:key       (:name field)
              :className "editing"}
         [dropdown/single-dropdown
          :src         (re-com/at)
          :choices     choices
          :model       *selected-val
          :width       "300px"
          :max-height  "200px"
          :class       "form-control"
          :style       {:padding "0"}
          :filter-box? true
          :on-change   #(let [valu (choice-id->val cid-type %)]
                          (reset! *selected-val %)
                          (rf/dispatch [:datagrid/update-edited-record id pk
                                        (:name field) valu]))]]))))

(comment
  ;; [:div.fg-line
  ;;  [:input.form-control {:type      "text"
  ;;                        :value     v
                         ;; :on-change #(rf/dispatch [:datagrid/update-edited-record id pk
                         ;;                           (:name field) (.-target.value %)])}]]

  )
