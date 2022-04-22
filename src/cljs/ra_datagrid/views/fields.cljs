(ns ra-datagrid.views.fields
  (:require [cljs-time.coerce :as coerce]
            [cljs-time.format :as fmt]
            [cljs-uuid-utils.core :as uuid]
            [clojure.string :as str]
            [medley.core :as medley]
            [ra-datagrid.config :as conf]
            [ra-datagrid.subs :as subs :refer [default-formatter]]
            [ra-datagrid.views :as views :refer [edit-cell table-cell]]
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
        *choices      (or (rf/subscribe (:data-subscription field))
                          (reagent/atom []))
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
;; Date)
;; -----

;; (Indent Group
;; -------------

(defmethod table-cell :indent-group
  [id {child->parent-field         :name
       {:keys [parent-subscription
               display-name-path]} :indent-group
       :as                         _field} record first?]
  (let [parent-id (get record child->parent-field)
        parent    @(rf/subscribe [parent-subscription parent-id])]
    (get-in parent display-name-path)))

(defmethod edit-cell :indent-group
  [id {child->parent-field         :name
       data-subscr                 :data-subscription
       {:keys [id-path
               display-name-path]} :indent-group
       :as                         _field} pk]
  (let [*r (rf/subscribe [:datagrid/edited-record-by-pk id pk])]
    (fn [id field pk]
      (let [id               (get-in @*r id-path)
            parent-id        (get @*r child->parent-field)
            records          @(rf/subscribe data-subscr)
            eligible-parents (->> records
                                  (remove #(let [ep-id        (get-in % id-path)
                                                 ep-parent-id (get-in % child->parent-field)]
                                             (or (nil? ep-id)   ;; If not saved to back-end
                                                 (= ep-id id)   ;; If option is this record
                                                 ep-parent-id   ;; If option is a child (has a parent)
                                                 (some (fn [r]  ;; If record has a child somewhere
                                                         (and id
                                                              (= (get r child->parent-field) id)))
                                                       records))))
                                  (sort-by (fn [r] (get-in r display-name-path))))]
        [:select.form-control {:value     (or parent-id "")
                               :on-change #(let [v (as-> % v
                                                     (.-target v)
                                                     (.-value v)
                                                     (if (and (string? v)
                                                              (str/blank? v))
                                                       nil
                                                       v)
                                                     (utils/maybe->uuid v))]
                                             (rf/dispatch [:datagrid/update-edited-record id pk
                                                           (:name field) v]))}
         ^{:key (str child->parent-field "-option-" id "-default-option")}
         [:option {:value ""} ""]
         (for [ep   eligible-parents
               :let [ep-id   (get-in ep id-path)
                     ep-name (get-in ep display-name-path)]]
           ^{:key (str child->parent-field "-option-" id "-" ep-id)}
           [:option {:value ep-id} ep-name])]))))

;; Indent Group)
;; -------------
