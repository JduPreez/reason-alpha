(ns reason-alpha.subs.positions
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.web.api-client :as api-client]
            [reason-alpha.utils :as utils]))

(rf/reg-sub
 :position/default-vals
 (fn [db _]
   (let [ps             (get-in db (data/entity-data :position))
         selected-id    (->> db (data/get-selected-ids :position) first)
         {parent-holding-pos-id
          :position-id} (some (fn [{:keys [holding-position-id position-id] :as p}]
                                (when (and (= position-id selected-id)
                                           (not holding-position-id))
                                  p))
                              ps)]
     {:position-creation-id (utils/new-uuid)
      :holding-position-id  parent-holding-pos-id})))

(rf/reg-sub
 :position/list
 (fn [db _]
   (sort-by (juxt :instrument-name :open-time)
            (get-in db data/positions))))

(rf/reg-sub
 :position/long-short-ref-list
 :<- [:model/members-of :model/position :position/long-short]
 (fn [{{:keys [enum/titles]} :properties
       mbrs                  :members} _]
   (for [m    mbrs
         :let [t (get titles m)]]
     {:id m :label t})))

(rf/reg-sub
 :position/long-short-titles
 :<- [:model/members-of :model/position :position/long-short]
 (fn [{{:keys [enum/titles]} :properties} _]
   titles))

(rf/reg-sub
 :position/holding-position-ref
 :<- [:position/list]
 (fn [positions [_ pid]]
   (some (fn [{:keys [position-id] :as pos}]
           (when (= position-id pid) pos))
         positions)))

(rf/reg-sub
 :position/holding-position-ref-list
 :<- [:position/list]
 (fn [positions _] positions))
