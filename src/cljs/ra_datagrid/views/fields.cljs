(ns ra-datagrid.views.fields
  (:require [cljs-time.coerce :as coerce]
            [cljs-time.format :as fmt]
            [cljs-uuid-utils.core :as uuid]
            [medley.core :as medley]
            [ra-datagrid.config :as conf]
            [ra-datagrid.subs :as subs :refer [default-formatter]]
            [ra-datagrid.views :as views :refer [edit-cell]]
            [re-com.core :as re-com]
            [re-com.datepicker :as datepicker]
            [re-com.dropdown :as dropdown]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reason-alpha.utils :as utils]))

;; (Select
;; -------

(defn choice-type [v]
  (cond
    (keyword? v)
    , :keyword

    (uuid/valid-uuid? v)
    , :uuid

    (medley/boolean? v)
    , :bool

    :else :string))

(defmethod default-formatter :select
  [{enum-titles :enum-titles}]
  (fn [[v1 v2] _]
    (case (choice-type v1)
      :keyword (get enum-titles v1)
      :uuid    v2
      (str v1))))

(defn choice-id [choices]
  (-> choices first :id))

(defn val->choice-id [ctype v]
  (if (= ctype :keyword)
    (utils/keyword->str v)
    (str v)))

(defn choice-id->val [ctype choices cid]
  (let [label (some (fn [{:keys [id label]}]
                      (when (= id cid) label)) choices)
        id    (case ctype
                :keyword (keyword cid)
                :uuid    (medley/uuid cid)
                :bool    (utils/str->bool cid)
                cid)]
    [id label]))

(defmethod edit-cell :select
  [id field pk]
  (let [*r            (rf/subscribe [:datagrid/edited-record-by-pk id pk])
        *choices      (rf/subscribe (:data-subscription field))
        ctype         (-> @*choices first :id choice-type)
        *selected-val (reagent/atom (->> field
                                         :name
                                         (get @*r)
                                         first
                                         (val->choice-id ctype)))]
    (fn [id field pk]
      (let [ctype   (-> @*choices first :id choice-type)
            choices (map (fn [{:keys [id] :as c}]
                           (update
                            c :id
                            #(val->choice-id ctype %)))
                         @*choices)]
        [:td {:key       (:name field)
              :className "editing"}
         [dropdown/single-dropdown
          :src         (re-com/at)
          :choices     choices
          :model       *selected-val
          :width       "100%"
          :max-height  "200px"
          :class       "form-control"
          :style       {:padding "0"}
          :filter-box? true
          :on-change   #(let [valu (choice-id->val ctype choices %)]
                          (reset! *selected-val %)
                          (rf/dispatch [:datagrid/update-edited-record id pk
                                        (:name field) valu]))]]))))
;; Select)
;; -------

;; (Date
;; -----

(defmethod edit-cell :date
  [id field pk]
  (let [*r             (rf/subscribe [:datagrid/edited-record-by-pk id pk])
        dte            (->> field :name (get @*r))
        *selected-date (reagent/atom
                        (when dte
                          (coerce/to-date-time dte)))]
    (fn [id field pk]
      [:td {:key       (:name field)
            :className "editing"}
       [datepicker/datepicker-dropdown
        :src           (re-com/at)
        :model         *selected-date
        :show-today?   true
        :show-weeks?   false
        :start-of-week 0
        :placeholder   "Select a date"
        :format        conf/date-format
        :width         "227px"
        :on-change     #(do
                          (reset! *selected-date %)
                          (rf/dispatch [:datagrid/update-edited-record id pk
                                        (:name field) (coerce/to-date %)]))]])))
