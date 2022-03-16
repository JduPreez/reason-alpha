(ns reason-alpha.events.trade-patterns
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.events :as events]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.api-client :as api-client]))

(rf/reg-event-fx
 :trade-pattern/load
 (fn [{:keys [db]} _]
   {:trade-pattern.query/getn nil
    :dispatch                 [:model.query/getn
                               [:model/trade-pattern
                                :model/trade-pattern-dto]]}))

(rf/reg-fx
 :trade-pattern.query/getn
 (fn [_]
   (cljs.pprint/pprint :trade-pattern.query/getn)
   (api-client/chsk-send! [:trade-pattern.query/getn]
                          {:on-success [:trade-pattern/getn-result]})))

(rf/reg-event-db
 :trade-pattern/getn-result
 (fn [db [evt {:keys [result type] :as r}]]
   (utils/log evt r)
   (if (= :success type)
     (data/save-local! {:db         db
                        :model-type :trade-pattern
                        :data       r})
     db)))

(rf/reg-event-fx
 :trade-pattern.command/add
 (fn [{:keys [db]} _]
   (let [trd-patrns     (get-in db (data/entity-data :trade-pattern))
         selected-ids   (data/get-selected-ids :trade-pattern db)
         selected-ents  (filter #(and (some #{(get % :trade-pattern/id)} selected-ids)
                                      (not (:trade-pattern/parent-id %)))
                                trd-patrns)
         new-trd-patrns (if (seq selected-ents)
                          (mapv (fn [tp]
                                  {:trade-pattern/creation-id (utils/new-uuid)
                                   :trade-pattern/parent-id   (:trade-pattern/id tp)
                                   :trade-pattern/name        ""
                                   :trade-pattern/description ""})
                                selected-ents)
                          [{:trade-pattern/creation-id (utils/new-uuid)
                            :trade-pattern/name        ""
                            :trade-pattern/description ""}])]
     {:dispatch [:edit new-trd-patrns]
      :db       (update-in db
                           data/trade-patterns
                           (fn [trd-patrns]
                             (into trd-patrns new-trd-patrns)))})))

(rf/reg-event-fx
 :trade-pattern.command/create
 (fn [{:keys [db]} [_ {:keys [creation-id] :as new-tpattern}]]
   (let [new-tpattern    (if creation-id
                           new-tpattern
                           (assoc new-tpattern :trade-pattern-creation-id
                                  (utils/new-uuid)))
         query-dto-model (get-in db (data/model :model/trade-pattern-dto))
         cmd-tpattern    (mapping/query-dto->command-ent query-dto-model new-tpattern)
         db              (data/save-local! {:model-type :trade-pattern
                                            :data       new-tpattern
                                            :db         db})]
     {:db                          db
      :trade-pattern.command/save! cmd-tpattern})))

(rf/reg-event-fx
 :trade-pattern.command/update
 (fn [{:keys [db]} [_ {:keys [creation-id] :as tpattern}]]
   (let [query-dto-model (get-in db (data/model :model/trade-pattern-dto))
         cmd-tpattern    (mapping/query-dto->command-ent query-dto-model tpattern)
         db              (data/save-local! {:model-type :trade-pattern
                                            :data       tpattern
                                            :db         db})]
     {:db                          db
      :trade-pattern.command/save! cmd-tpattern})))

(rf/reg-fx
 :trade-pattern.command/save!
 (fn [tpattern]
   (utils/log :trade-pattern.command/save! tpattern)
   (data/save-remote! {:command       :trade-pattern.command/save!
                       :data          tpattern
                       :success-event [:trade-pattern.command/save!-result]})))

(rf/reg-event-db
 :trade-pattern.command/save!-result
 (fn [db [evt {:keys [type result] :as r}]]
   (utils/log evt r)
   (if (= :success type)
     (data/save-local! {:db         db
                        :model-type :trade-pattern
                        :data       result})
     db)))

(rf/reg-fx
 :trade-pattern.command/delete!
 (fn [db]
   (let [del-ids (data/get-selected-ids :trade-pattern db)]
     (api-client/chsk-send! [:trade-pattern.command/delete! del-ids]
                            {:on-success [:trade-pattern/delete!-result]}))))

(rf/reg-event-fx
 :trade-pattern/delete!-result
 (fn [{:keys [db]} [_ data]]
   (data/delete-local! {:db         db
                        :model-type :trade-pattern
                        :data       data})))




