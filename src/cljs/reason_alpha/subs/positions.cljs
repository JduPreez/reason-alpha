(ns reason-alpha.subs.positions
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.web.api-client :as api-client]
            [reason-alpha.utils :as utils]))

(rf/reg-sub
 :position/list
 (fn [db _]
   (sort-by (juxt :instrument-name :open-time)
            (get-in db data/positions))))

(rf/reg-sub
 :position/long-short-ref-list
 :<- [:models/members-of :model/position :position/long-short]
 (fn [{{:keys [enum/titles]} :properties
       mbrs                  :members} _]
   (for [m    mbrs
         :let [t (get titles m)]]
     {:id m :label t})))

(rf/reg-sub
 :position/long-short-titles
 :<- [:models/members-of :model/position :position/long-short]
 (fn [{{:keys [enum/titles]} :properties} _]
   titles))
