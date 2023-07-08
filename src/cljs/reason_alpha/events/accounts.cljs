(ns reason-alpha.events.accounts
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.model.utils :as mutils]
            [reason-alpha.utils :as utils]
            [reason-alpha.views.accounts :as views.accounts]
            [reason-alpha.web.api-client :as api-client]))

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
        :dispatch [:view.data/init
                   ::views.accounts/account-edit
                   :account acc]})
     {:db db})))

(rf/reg-event-fx
 :account/save
 (fn [{:keys [db]} [evt acc]]
   (let [qry-dto-model (get-in db (data/model :model/account-dto))
         cmd-a         (mapping/query-dto->command-ent qry-dto-model acc)
         db            (data/save-local! {:model-type :account
                                          :data       acc
                                          :db         db})]
     {:db            db
      :dispatch      [:close-active-form]
      :account/save! cmd-a})))

(rf/reg-fx
 :account/save!
 (fn [a]
   (utils/log :account.command/save! a)
   (data/save-remote! {:command :account.command/save!
                       :data    a})))
