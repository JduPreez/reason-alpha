(ns reason-alpha.subs.account-profile
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]))

(rf/reg-sub
 :account-profile
 (fn [db _]
   (->> data/account-profile
        (get-in db)
        first)))
