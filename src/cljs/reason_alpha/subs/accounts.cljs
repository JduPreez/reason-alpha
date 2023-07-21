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

(rf/reg-sub
 :account/without-ref-titles
 (fn [db _]
   (-> db (get-in data/accounts) first)))

(rf/reg-sub
 :account
 (fn [_]
   [(rf/subscribe [:account/without-ref-titles])
    (rf/subscribe [:financial-instrument/currencies])])
 (fn [[{[cid _] :account-currency :as acc} currencies] _]
   (assoc acc :account-currency-nm
          (get currencies cid))))
