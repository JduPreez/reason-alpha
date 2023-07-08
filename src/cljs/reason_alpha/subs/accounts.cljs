(ns reason-alpha.subs.accounts
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.model.utils :as mutils]))

(rf/reg-sub
 :account/currency-ref-list
 :<- [:model/members-of :model/account :account/currency]
 (fn [{currency-sch :schema} _]
   (->> currency-sch
        mutils/enum-titles
        (map (fn [[id title]]
               {:id    id
                :label title})))))
