(ns reason-alpha.subs.instruments
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]))

(rf/reg-sub
 :instrument/type-ref-list
 :<- [:models/members-of :model/instrument :instrument/type]
 (fn [{{:keys [enum/titles]} :properties
       mbrs                  :members
       :as                   instr-types} _]
   (into {} (for [m    mbrs
                  :let [t (get titles m)]]
              [(name m) t]))))

(rf/reg-sub
 :instrument/list
 (fn [db _]
   (cljs.pprint/pprint {::list (get-in db data/instruments)})
   (sort-by :instrument-name
            (get-in db data/instruments))))
