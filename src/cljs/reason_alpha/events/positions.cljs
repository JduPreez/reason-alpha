(ns reason-alpha.events.positions
  (:require [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.api-client :as api-client]))

(rf/reg-event-fx
 :position/load
 (fn [{:keys [db]} _]
   {:position.query/getn   nil
    :instrument.query/getn nil
    :dispatch              [:model.query/getn
                            [:model/position :model/position-dto]]}))

(rf/reg-fx
 :position.query/getn
 (fn [_]
   (api-client/chsk-send! [:position.query/getn])))

(rf/reg-event-db
 :position.query/getn-result
 (fn [db [evt {:keys [result type] :as r}]]
   (utils/log evt r)
   (if (= :success type)
     (data/save-local! {:db         db
                        :model-type :position
                        :data       result})
     db)))

(rf/reg-fx
 :position.query/get1
 (fn [pos-id]
   (cljs.pprint/pprint {:position.query/get1 pos-id})
   (api-client/chsk-send! [:position.query/get1 pos-id])))

(rf/reg-event-db
 :position.query/get1-result
 (fn [db [evt {:keys [result type] :as r}]]
   (utils/log evt r)
   (if (= :success type)
     (data/save-local! {:db         db
                        :model-type :position
                        :data       result})
     db)))

(rf/reg-event-fx
 :position.command/create
 (fn [{:keys [db]} [_ {:keys [creation-id] :as new-pos}]]
   (let [new-pos         (if creation-id
                           new-pos
                           (assoc new-pos
                                  :position-creation-id
                                  (utils/new-uuid)))
         query-dto-model (get-in db (data/model :model/position-dto))
         cmd-pos         (mapping/query-dto->command-ent query-dto-model new-pos)
         db              (data/save-local! {:model-type :position
                                            :data       new-pos
                                            :db         db})]
     (cljs.pprint/pprint {:position.command/create {:M  query-dto-model
                                                    :NP new-pos
                                                    :CP cmd-pos}})
     {:db                     db
      :position.command/save! cmd-pos})))

(rf/reg-event-fx
 :position.command/update
 (fn [{:keys [db]} [_ {:keys [creation-id] :as pos}]]
   (let [query-dto-model (get-in db (data/model :model/position-dto))
         cmd-pos         (mapping/query-dto->command-ent query-dto-model pos)
         db              (data/save-local! {:model-type :position
                                            :data       pos
                                            :db         db})]
     {:db                     db
      :position.command/save! cmd-pos})))

(rf/reg-event-fx
 :position.command/save!-result
 (fn [_ [evt {:keys [type result] :as r}]]
   (utils/log evt r)
   (when (= :success type)
     {:position.query/get1 (:position/id result)})))

(rf/reg-fx
 :position.command/save!
 (fn [pos]
   (utils/log :position.command/save! pos)
   (data/save-remote! {:command :position.command/save!
                       :data    pos})))
