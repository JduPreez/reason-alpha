(ns ra-datagrid.views.fields
  (:require [cljs-time.coerce :as coerce]
            [cljs-uuid-utils.core :as uuid]
            [clojure.string :as str]
            [medley.core :as medley]
            [ra-datagrid.config :as conf]
            [ra-datagrid.subs :as subs :refer [default-formatter]]
            [ra-datagrid.views :as views :refer [edit-cell table-cell]]
            [ra-datagrid.views.components.datepicker :as datepicker]
            [re-com.core :as re-com]
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
  (when cid
    (let [label (some (fn [{:keys [id label]}]
                        (when (= id cid) label)) choices)
          id    (case ctype
                  :keyword (keyword cid)
                  :uuid    (medley/uuid cid)
                  :bool    (utils/str->bool cid)
                  cid)]
      [id label])))

(defmethod edit-cell :select
  [grid-id field pk]
  (let [*r            (rf/subscribe [:datagrid/edited-record-by-pk-with-validation grid-id pk])
        *choices      (or (rf/subscribe (:data-subscription field))
                          (reagent/atom []))
        ctype         (-> @*choices first :id choice-type)
        *selected-val (reagent/atom (-> @*r
                                        :result
                                        (get (:name field))
                                        first
                                        (as-> v (val->choice-id ctype v))))]
    (fn [grid-id field pk]
      (let [{field-nm :name
             opt?     :optional?} field
            validation            (-> @*r :validation (get field-nm))
            ctype                 (-> @*choices first :id choice-type)
            choices               (map (fn [{:keys [grid-id] :as c}]
                                         (update
                                          c :id
                                          #(val->choice-id ctype %)))
                                       @*choices)
            choices               (if opt?
                                    (cons {:id nil, :label [:div {:style {:margin-top    "1rem"
                                                                          :margin-bottom "1rem"}}]} choices)
                                    #_else choices)]
        [:td {:key       (:name field)
              :className "editing data-cell"}
         [dropdown/single-dropdown
          :src         (re-com/at)
          :choices     choices
          :model       *selected-val
          :width       "100%"
          :max-height  "200px"
          :class       (cond-> "form-control"
                         (seq validation) (str " is-invalid"))
          :style       {:padding "0"}
          :filter-box? true
          :on-change   #(let [valu (choice-id->val ctype choices %)]
                          (reset! *selected-val %)
                          (rf/dispatch [:datagrid/update-edited-record grid-id pk
                                        (:name field) valu]))]
         (views/invalid-feedback validation)]))))

;; Select)
;; -------

;; (Date
;; -----

(defmethod edit-cell :date
  [grid-id field pk]
  (let [;;*r             (rf/subscribe [:datagrid/edited-record-by-pk grid-id pk])
        *r             (rf/subscribe [:datagrid/edited-record-by-pk-with-validation grid-id pk])
        dte            (->> @*r :result (:name field) (get @*r))
        *selected-date (reagent/atom
                        (when dte
                          (coerce/to-date-time dte)))]
    (fn [grid-id field pk]
      (let [validation (-> @*r :validation (get (:name field)))]
        [:td {:key       (:name field)
              :className "editing data-cell"}
         [datepicker/datepicker-dropdown
          :src           (re-com/at)
          :model         *selected-date
          :show-today?   true
          :show-weeks?   false
          :start-of-week 0
          :placeholder   "Select a date"
          :format        conf/date-format
          :width         "227px"
          :invalid-feedback validation ;;(when (seq validation) "is-invalid")
          :parts {:wrapper {:class "form-control is-invalid"}}
          :on-change     #(do
                            (reset! *selected-date %)
                            (rf/dispatch [:datagrid/update-edited-record grid-id pk
                                          (:name field) (coerce/to-date %)]))]]))))
;; Date)
;; -----

;; (Indent Group
;; -------------

(defmethod table-cell :indent-group
  [id field record indent?]
  (let [*options (rf/subscribe [:datagrid/options id])]
    (fn [id field record indent?]
      (let [{:keys [parent-subscription
                    display-name-path]} (:indent-group field)
            is-clickable?               (not (nil? (:on-click field)))
            formatter                   (:formatter field)
            fieldname                   (:name field)
            parent-id                   (get record fieldname)
            parent                      @(rf/subscribe [parent-subscription parent-id])
            formatted-value             (get-in parent display-name-path)
            align                       (if (nil? (:align field)) :text-left (:align field))]
        [:td (cond-> {:key       fieldname
                      :className align}
               indent? (assoc :style {:padding-left "30px"}))
         formatted-value]))))

(defmethod edit-cell :indent-group
  [grid-id {member-key                  :name
            data-subscr                 :data-subscription
            {:keys [group-path
                    display-name-path]} :indent-group
            :as                         _field} pk]
  (let [*r (rf/subscribe [:datagrid/edited-record-by-pk-with-validation grid-id pk])]
    (fn [grid-id field pk]
      (let [validation       (-> @*r :validation (get (:name field)))
            id               (-> @*r :result (get-in group-path))
            parent-id        (-> @*r :result (get member-key))
            records          @(rf/subscribe data-subscr)
            eligible-parents (->> records
                                  (remove #(let [ep-id        (get-in % group-path)
                                                 ep-parent-id (get % member-key)]
                                             (or (nil? ep-id)   ;; If not saved to back-end
                                                 (= ep-id id)   ;; If option is this record
                                                 ep-parent-id   ;; If option is a child (has a parent)
                                                 (some (fn [r]  ;; If record has a child somewhere
                                                         (and id
                                                              (= (get r member-key) id)))
                                                       records))))
                                  (sort-by (fn [r] (get-in r display-name-path))))]
        [:td {:key       member-key
              :className "editing"}
         [:select.form-control {:value     (or parent-id "")
                                :on-change #(let [v (as-> % v
                                                      (.-target v)
                                                      (.-value v)
                                                      (if (and (string? v)
                                                               (str/blank? v))
                                                        nil
                                                        v)
                                                      (utils/maybe->uuid v))]
                                              (rf/dispatch [:datagrid/update-edited-record grid-id pk
                                                            member-key v]))}
          ^{:key (str member-key "-option-" id "-default-option")}
          [:option {:value ""} ""]
          (for [ep   eligible-parents
                :let [ep-id   (get-in ep group-path)
                      ep-name (get-in ep display-name-path)]]
            ^{:key (str member-key "-option-" id "-" ep-id)}
            [:option {:value ep-id} ep-name])]
         (views/invalid-feedback validation)]))))

;; Indent Group)
;; -------------
