(ns reason-alpha.views.components
  (:require [cljs-uuid-utils.core :as uuid]
            [medley.core :as medley]
            [re-com.core :as re-com]
            [re-com.dropdown :as dropdown]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reason-alpha.model.utils :as mutils]
            [reason-alpha.utils :as utils]
            [reason-alpha.views.utils :as vutils]))

(defn meta-name
  [fn?]
  (-> fn? meta :name))

(defn val->choice-id [ctype v]
  (if (= ctype (meta-name #'keyword?))
    (utils/keyword->str v)
    (str v)))

(defn choice-id->val [ctype choices cid]
  (let [label (some (fn [{:keys [id label]}]
                      (when (= id cid) label)) choices)
        id    (cond
                (= ctype (meta-name #'keyword?)) (keyword cid)
                (= ctype (meta-name #'uuid?))    (medley/uuid cid)
                (= ctype (meta-name #'boolean?)) (utils/str->bool cid)
                :else                            cid)]
    [id label]))

(defn select-dropdown
  [*ent & {:keys [schema model-type member-nm]}]
  (let [{m-v-type :member-val-type} (mutils/model-member-schema-info schema member-nm)
        *selected-val               (reagent/atom (->> member-nm
                                                       (get @*ent)
                                                       first
                                                       (val->choice-id m-v-type)))]
    (fn [*ent & {:keys [schema model-type member-nm selected view]}]
      (let [id                          (->> schema
                                             (mutils/id-key model-type)
                                             (get @*ent))
            {{r :ref} :properties
             m-v-type :member-val-type} (mutils/model-member-schema-info schema member-nm)
            data-subscription           (vutils/ref->data-sub r)

            *choices (if data-subscription
                       (rf/subscribe data-subscription)
                       (reagent/atom []))
            choices  (map #(update % :id
                                   (partial val->choice-id m-v-type))
                          @*choices)]
        [dropdown/single-dropdown
         :src         (re-com/at)
         :choices     choices
         :model       *selected-val
         :width       "100%"
         :max-height  "200px"
         :class       "form-control"
         :style       {:padding "0"}
         :filter-box? true
         :on-change   #(let [v (choice-id->val m-v-type choices %)]
                         (reset! *selected-val %)
                         (rf/dispatch [selected view member-nm v]))]))))
