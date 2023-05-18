(ns reason-alpha.events.account-profile
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.api-client :as api-client]))

(rf/reg-event-fx
 :account-profile/load
 (fn [{:keys [db]} _]
   {:account-profile/get1 nil
    :dispatch             [:model.query/getn
                           [:model/currency
                            :model/account-dto
                            :model/account]]}))

(rf/reg-fx
 :account-profile/get1
 (fn [_]
   (api-client/chsk-send! [:account.query/get1])))

(rf/reg-event-db
 :account.query/get1-result
 (fn [db [evt {:keys [result type] :as r}]]
   (utils/log evt r)
   (if (= :success type)
     (data/save-local! {:db         db
                        :model-type :account-profile
                        :data       result})
     db)))
