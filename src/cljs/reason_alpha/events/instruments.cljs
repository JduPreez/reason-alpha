(ns reason-alpha.events.instruments
  (:require [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.model.utils :as model.utils]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.api-client :as api-client]))

(rf/reg-event-fx
 :instrument/load
 (fn [{:keys [db]} _]
   {:instrument.query/getn nil
    :dispatch              [:model.query/getn
                            [:model/instrument :model/symbol
                             :model/instrument-dto]]}))

(rf/reg-fx
 :instrument.query/getn
 (fn [_]
   (cljs.pprint/pprint :instrument.query/getn)
   (api-client/chsk-send! [:instrument.query/getn])))

(rf/reg-event-db
 :instrument.query/getn-result
 (fn [db [evt {:keys [result type] :as r}]]
   (utils/log evt r)
   (if (= :success type)
     (data/save-local! {:db         db
                        :model-type :instrument
                        :data       result})
     db)))

(rf/reg-event-db
 :instrument.query/get1-result
 (fn [db [evt {:keys [result type] :as r}]]
   (utils/log evt r)
   (if (= :success type)
     (data/save-local! {:db         db
                        :model-type :instrument
                        :data       result})
     db)))

(rf/reg-fx
 :instrument.query/get1
 (fn [instr-id]
   (cljs.pprint/pprint {:instrument.query/get1 instr-id})
   (api-client/chsk-send! [:instrument.query/get1 {:instrument-id instr-id}])))

(rf/reg-event-fx
 :instrument.command/save!-result
 (fn [_ [evt {:keys [type result] :as r}]]
   (utils/log evt r)
   (when (= :success type)
     {:instrument.query/get1 (:instrument/id result)})))

(rf/reg-fx
 :instrument.command/save!
 (fn [instr]
   (utils/log :instrument.command/save! instr)
   (data/save-remote! {:command :instrument.command/save!
                       :data    instr})))

(rf/reg-event-fx
 :instrument.command/create
 (fn [{:keys [db]} [_ {:keys [creation-id] :as new-instr}]]
   (let [new-instr       (if creation-id
                           new-instr
                           (assoc new-instr :instrument-creation-id (utils/new-uuid)))
         query-dto-model (get-in db (data/model :model/instrument-dto))
         cmd-instr       (mapping/query-dto->command-ent query-dto-model new-instr)
         db              (data/save-local! {:model-type :instrument
                                            :data       new-instr
                                            :db         db})]
     {:db                       db
      :instrument.command/save! cmd-instr})))

(rf/reg-event-fx
 :instrument.command/update
 (fn [{:keys [db]} [_ {:keys [creation-id] :as instr}]]
   (let [query-dto-model (get-in db (data/model :model/instrument-dto))
         cmd-instr       (mapping/query-dto->command-ent query-dto-model instr)
         db              (data/save-local! {:model-type :instrument
                                            :data       instr
                                            :db         db})]
     {:db                       db
      :instrument.command/save! cmd-instr})))

(rf/reg-event-fx
 :instrument.command/delete!-result
 (fn [{:keys [db]} [evt result]]
   (utils/log evt result)
   (data/delete-local! {:db         db
                        :model-type :instrument
                        :data       result})))

(rf/reg-fx
 :instrument.command/delete!
 (fn [db]
   (let [del-ids (data/get-selected-ids :instrument db)]
     (api-client/chsk-send! [:instrument.command/delete! del-ids]))))
