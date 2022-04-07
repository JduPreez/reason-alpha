(ns reason-alpha.events.holdings
  (:require [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.model.utils :as model.utils]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.api-client :as api-client]))

(rf/reg-event-fx
 :holding/load
 (fn [{:keys [db]} _]
   {:holding.query/get-holdings nil
    :dispatch                   [:model.query/getn
                                 [:model/holding :model/symbol
                                  :model/holding-dto]]}))

(rf/reg-fx
 :holding.query/get-holdings
 (fn [_]
   (api-client/chsk-send! [:holding.query/get-holdings])))

(rf/reg-event-db
 :holding.query/get-holdings-result
 (fn [db [evt {:keys [result type] :as r}]]
   (utils/log evt r)
   (if (= :success type)
     (data/save-local! {:db         db
                        :model-type :holding
                        :data       result})
     db)))

(rf/reg-event-db
 :holding.query/get-holding-result
 (fn [db [evt {:keys [result type] :as r}]]
   (utils/log evt r)
   (if (= :success type)
     (data/save-local! {:db         db
                        :model-type :holding
                        :data       result})
     db)))

(rf/reg-fx
 :holding.query/get-holding
 (fn [id]
   (api-client/chsk-send! [:holding.query/get-holding id])))

(rf/reg-event-fx
 :holding.command/save-holding!-result
 (fn [_ [evt {:keys [type result] :as r}]]
   (utils/log evt r)
   (when (= :success type)
     {:holding.query/get-holding (:holding/id result)})))

(rf/reg-fx
 :holding.command/save-holding!
 (fn [ent]
   (utils/log :holding.command/save-holding! ent)
   (data/save-remote! {:command :holding.command/save-holding!
                       :data    ent})))

(rf/reg-event-fx
 :holding.command/create
 (fn [{:keys [db]} [_ {:keys [creation-id] :as new-ent}]]
   (let [new-ent         (if creation-id
                           new-ent
                           (assoc new-ent :holding-creation-id (utils/new-uuid)))
         query-dto-model (get-in db (data/model :model/holding-dto))
         cmd-ent         (mapping/query-dto->command-ent query-dto-model new-ent)
         db              (data/save-local! {:model-type :holding
                                            :data       new-ent
                                            :db         db})]
     {:db                            db
      :holding.command/save-holding! cmd-ent})))

(rf/reg-event-fx
 :holding.command/update
 (fn [{:keys [db]} [_ {:keys [creation-id] :as ent}]]
   (let [query-dto-model (get-in db (data/model :model/holding-dto))
         cmd-ent         (mapping/query-dto->command-ent query-dto-model ent)
         db              (data/save-local! {:model-type :holding
                                            :data       cmd-ent
                                            :db         db})]
     {:db                    db
      :holding.command/save! cmd-ent})))

(rf/reg-event-fx
 :holding.command/delete!-result
 (fn [{:keys [db]} [evt result]]
   (utils/log evt result)
   (data/delete-local! {:db         db
                        :model-type :holding
                        :data       result})))

(rf/reg-fx
 :holding.command/delete!
 (fn [db]
   (let [del-ids (data/get-selected-ids :holding db)]
     (api-client/chsk-send! [:holding.command/delete-holding! del-ids]))))
