(ns reason-alpha.subs.instruments
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.utils :as utils]))

(rf/reg-sub
 :instrument/type-ref-list
 :<- [:models/members-of :model/instrument :instrument/type]
 (fn [{{:keys [enum/titles]} :properties
       mbrs                  :members
       :as                   instr-types} _]
   (for [m    mbrs
         :let [t (get titles m)]]
     {:id (utils/keyword->str m) :label t})))

(rf/reg-sub
 :instrument/type-titles
 :<- [:models/members-of :model/instrument :instrument/type]
 (fn [{{:keys [enum/titles]} :properties} _]
   titles))

(rf/reg-sub
 :instrument/list
 (fn [db _]
   (sort-by :instrument-name
            (get-in db data/instruments))))