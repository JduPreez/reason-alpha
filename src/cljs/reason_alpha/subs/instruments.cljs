(ns reason-alpha.subs.instruments
  (:require [re-frame.core :as rf]))

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
 :instrument/options
 (fn [db _]
   []))
