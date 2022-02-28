(ns reason-alpha.subs.instruments
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]))

(rf/reg-sub
 :instrument/types
 :<- [:models/members-of :model/instrument :instrument/type]
 (fn [{{:keys [enum/titles]} :properties
       mbrs                  :members
       :as                   instr-types} _]
   (for [m    mbrs
         :let [t (get titles m)]]
     {:value m
      :text  t})))

(rf/reg-sub
 :instrument/ref-list
 (fn [db _]
   []))

(rf/reg-sub
 :instrument/list
 (fn [db _]
   (cljs.pprint/pprint {::list (get-in db data/instruments)})
   (sort-by :instrument-name
            (get-in db data/instruments))))
