(ns reason-alpha.subs.holdings
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.utils :as utils]))

(rf/reg-sub
 :holding/instrument-type-ref-list
 :<- [:models/members-of :model/holding :holding/instrument-type]
 (fn [{{:keys [enum/titles]} :properties
       mbrs                  :members
       :as                   _holding-types} _]
   (for [m    mbrs
         :let [t (get titles m)]]
     {:id m :label t})))

(rf/reg-sub
 :holding/instrument-type-titles
 :<- [:models/members-of :model/holding :holding/instrument-type]
 (fn [{{:keys [enum/titles]} :properties} _]
   titles))

(rf/reg-sub
 :holding/list
 (fn [db _]
   (sort-by :instrument-name
            (get-in db data/holdings))))

(rf/reg-sub
 :holding/ref-list
 :<- [:holding/list]
 (fn [holdings _]
   (map
    (fn [{:keys [holding-id instrument-name]}]
      {:id    (str holding-id)
       :label instrument-name})
    holdings)))
