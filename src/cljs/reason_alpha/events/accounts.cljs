(ns reason-alpha.events.accounts
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.api-client :as api-client]
            [reason-alpha.views.accounts :as views.accounts]))

(rf/reg-event-fx
 :account/load
 (fn [{:keys [db]} _]
   {:account/get1 nil
    :dispatch     [:model.query/getn
                   [:model/currency
                    :model/account-dto
                    :model/account]]}))

(rf/reg-fx
 :account/get1
 (fn []
   (api-client/chsk-send! [:account.query/get1])))

(rf/reg-event-fx
 :account.query/get1-result
 (fn [{:keys [db]} [evt {:keys [result type] :as r}]]
   (utils/log evt r)
   (if (= :success type)
     (let [{acc :saved
            :as db} (data/save-local! {:db         db
                                       :model-type :account
                                       :data       result})]
       {:db       db
        :dispatch [:init-view-data
                   ::views.accounts/account-edit acc]})
     {:db db})))

;; TODO: Move to generic ns
(rf/reg-event-db
 :update-view-data
 (fn [db [_ view field value]]
   (data/update-view-data db view field value)))

(rf/reg-event-db
 :init-view-data
 (fn [db [_ view entity]]
   (data/init-view-data db view entity)))
