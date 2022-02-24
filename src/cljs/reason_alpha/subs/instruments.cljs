(ns reason-alpha.subs.instruments
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :instruments/types
 :<- [:models/members-of :model/instrument :instrument/type]
 (fn [{{:keys [enum/titles]} :properties
       mbrs                  :members
       :as                   instr-types} _]
   (for [m    mbrs
         :let [t (get titles m)]]
     {:value m
      :text  t})))

(rf/reg-sub
 :instruments
 (fn [db _]
   []))
