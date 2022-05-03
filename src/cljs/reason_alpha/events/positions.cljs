(ns reason-alpha.events.positions
  (:require [clojure.set :as set]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.api-client :as api-client]))

(rf/reg-event-fx
 :position/load
 (fn [{:keys [db]} _]
   {:holding.query/get-holdings-positions nil
    :holding.query/get-holdings           nil
    :trade-pattern.query/getn             nil
    :dispatch                             [:model.query/getn
                                           [:model/position :model/position-dto]]}))

(rf/reg-fx
 :holding.query/get-holdings-positions
 (fn [_]
   (api-client/chsk-send! [:holding.query/get-holdings-positions])))

(rf/reg-event-db
 :holding.query/get-holdings-positions-result
 (fn [db [evt {:keys [result type] :as r}]]
   (utils/log evt r)
   (if (= :success type)
     (data/save-local! {:db         db
                        :model-type :position
                        :data       result})
     db)))

(rf/reg-fx
 :holding.query/get-holding-positions
 (fn [pos-id]
   (api-client/chsk-send! [:holding.query/get-holding-positions pos-id])))

(rf/reg-event-db
 :holding.query/get-holding-positions-result
 (fn [db [evt {:keys [result type] :as r}]]
   (utils/log evt r)
   (if (= :success type)
     (data/save-local! {:db         db
                        :model-type :position
                        :data       result})
     db)))

(rf/reg-event-fx
 :position/create
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
     #_{:db                             db
      :holding.command/save-position! cmd-pos})))

(rf/reg-event-fx
 :position/update
 (fn [{:keys [db]} [_ {:keys [creation-id] :as pos}]]
   (let [query-dto-model (get-in db (data/model :model/position-dto))
         cmd-pos         (mapping/query-dto->command-ent query-dto-model pos)
         db              (data/save-local! {:model-type :position
                                            :data       pos
                                            :db         db})]
     {:db                             db
      :holding.command/save-position! cmd-pos})))

(rf/reg-event-fx
 :holding.command/save-position!-result
 (fn [_ [evt {:keys [type result] :as r}]]
   (utils/log evt r)
   (when (= :success type)
     {:position.query/get-position (:position/id result)})))

(rf/reg-fx
 :holding.command/save-position!
 (fn [pos]
   (utils/log :holding.command/save-position! pos)
   (data/save-remote! {:command :holding.command/save-position!
                       :data    pos})))

(rf/reg-event-fx
 :holding.command/delete-positions!-result
 (fn [{:keys [db]} [evt result]]
   (utils/log evt result)
   (data/delete-local! {:db         db
                        :model-type :position
                        :data       result})))

(rf/reg-fx
 :holding.command/delete-positions!
 (fn [db]
   (let [del-ids (data/get-selected-ids :position db)]
     (api-client/chsk-send! [:holding.command/delete-positions! del-ids]))))

#_(rf/reg-event-db
 :price/quotes
 (fn [db [evt result]]
   (let [prices    (into {} (map (fn [{:keys [holding-id price-close]}]
                                   [holding-id price-close]) result))
         positions (->> data/positions
                        (get-in db)
                        (map (fn [{[price-hid _] :holding
                                   cest?         :close-estimated?
                                   :as           pos}]
                               (if-let [price-close (and cest?
                                                         (get prices price-hid))]
                                 (assoc pos :close-price price-close)
                                 pos))))]
     (assoc-in db data/positions positions))))
