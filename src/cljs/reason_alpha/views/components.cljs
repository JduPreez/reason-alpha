(ns reason-alpha.views.components
  (:require [cljs-uuid-utils.core :as uuid]
            [medley.core :as medley]
            [re-com.core :as re-com]
            [re-com.dropdown :as dropdown]
            [re-com.dropdown :as dropdown]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reason-alpha.model.utils :as mutils]
            [reason-alpha.utils :as utils]))

(defn val->choice-id [ctype v]
  (if (= ctype :keyword)
    (utils/keyword->str v)
    (str v)))

(defn choice-type [v]
  (cond
    (keyword? v)
    , :keyword

    (uuid/valid-uuid? v)
    , :uuid

    (medley/boolean? v)
    , :bool

    :else :string))

(defn choice-id->val [ctype choices cid]
  (let [label (some (fn [{:keys [id label]}]
                      (when (= id cid) label)) choices)
        id    (case ctype
                :keyword (keyword cid)
                :uuid    (medley/uuid cid)
                :bool    (utils/str->bool cid)
                cid)]
    [id label]))

(defn select-dropdown
  [& {:keys [schema model-type member-nm data-subscription
             enum-titles]}]
  (let [id-member (mutils/id-key model-type schema)
        {:keys [properties
                schema
                members]
         :as   x}  (mutils/model-member-schema-info schema member-nm)]
    (cljs.pprint/pprint {:>>>X  x
                         :>>>MK member-nm
                         :>>>IM id-member
                         :>>>S  schema}))
  #_([model-type idk *r {data-sub :data-subscription
                         :as      field}]
     (let [id            (idk @*r)
           ;; *r                (rf/subscribe [:form/edited-record-by-pk form id])
           *choices      (if data-sub
                           (rf/subscribe data-sub)
                           #_else (reagent/atom []))
           ctype         (-> @*choices first :id choice-type)
           *selected-val (reagent/atom (->> field
                                            :name
                                            (get @*r)
                                            first
                                            (val->choice-id ctype)))]
     [:span @*selected-val]
     #_(fn [form model-type idk *r _]
         (let [choices (map (fn [{:keys [id] :as c}]
                              (update
                               c :id
                               #(val->choice-id ctype %)))
                            @*choices)]
           (cljs.pprint/pprint {::select-dropdown choices})
           [dropdown/single-dropdown
            :src (re-com/at)
            :choices choices
            :model       *selected-val
            :width       "100%"
            :max-height  "200px"
            :class       "form-control"
            :style       {:padding "0"}
            :filter-box? true])))))
