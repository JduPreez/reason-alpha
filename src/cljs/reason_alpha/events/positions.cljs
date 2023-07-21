(ns reason-alpha.events.positions
  (:require [clojure.set :as set]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.events.common :as common]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.api-client :as api-client]))

(rf/reg-event-fx
 :position/load
 (fn [{:keys [db]} _]
   {:position/get-holdings-positions nil
    :holding/get-holdings            nil
    :trade-pattern.query/getn        nil
    :dispatch-n                      [[:account/load]
                                      [:model.query/getn
                                       [:model/position :model/position-dto]]]}))

(rf/reg-fx
 :position/get-holdings-positions
 (fn [_]
   (api-client/chsk-send! [:holding.query/get-holdings-positions])))

(rf/reg-event-fx
 :holding.query/get-holdings-positions-result
 (fn [{:keys [db]} [evt {:keys [result type] :as r}]]
   (utils/log evt r)
   (if (= :success type)
     {:db (data/save-local! {:db         db
                             :model-type :position
                             :data       result})}
     {:db       db
      :dispatch [:alert/send r]})))

(rf/reg-fx
 :position/get-holding-positions
 (fn [pos-id]
   (api-client/chsk-send! [:holding.query/get-holding-positions pos-id])))

(rf/reg-event-fx
 :holding.query/get-holding-positions-result
 (fn [{:keys [db]} [evt {:keys [result type] :as r}]]
   (utils/log evt r)
   (if (= :success type)
     {:db (data/save-local! {:db         db
                             :model-type :position
                             :data       result})}
     {:db       db
      :dispatch [:alert/send r]})))

(rf/reg-event-fx
 :position/create
 (fn [{:keys [db]} [_ {:keys [creation-id close-price] :as new-pos}]]
   (let [new-pos         (if creation-id
                           new-pos
                           (assoc new-pos
                                  :position-creation-id
                                  (utils/new-uuid)))
         new-pos         (if close-price
                           (assoc new-pos :status :closed)
                           (assoc new-pos :status :open))
         query-dto-model (get-in db (data/model :model/position-dto))
         cmd-pos         (mapping/query-dto->command-ent query-dto-model new-pos)
         db              (data/save-local! {:model-type :position
                                            :data       new-pos
                                            :db         db})]
     {:db             db
      :position/save! cmd-pos})))

(rf/reg-event-fx
 :position/update
 (fn [{:keys [db]} [_ {:keys [creation-id close-price position-id] :as pos}]]
   (let [{cur-close-pr :close-price} (data/get-entity db :position {:position-id position-id})
         status                      (if (and close-price
                                              (not= close-price cur-close-pr))
                                       :closed
                                       :open)
         pos                         (assoc pos :status status)
         query-dto-model             (get-in db (data/model :model/position-dto))
         cmd-pos                     (mapping/query-dto->command-ent query-dto-model pos)
         db                          (data/save-local! {:model-type :position
                                                        :data       pos
                                                        :db         db})]
     {:db             db
      :position/save! cmd-pos})))

(rf/reg-event-fx
 :holding.command/save-position!-result
 (fn [_ [evt {:keys [type result] :as r}]]
   (utils/log evt r)
   (when (= :success type)
     {:position/get-holding-positions result})))

(rf/reg-fx
 :position/save!
 (fn [pos]
   (utils/log :holding.command/save-position! pos)
   (data/save-remote! {:command :holding.command/save-position!
                       :data    pos})))

(rf/reg-event-fx
 :holding.command/delete-positions!-result
 (common/handle-delete!-result-fn "positions" :position))

(rf/reg-fx
 :position/delete!
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
                                   status        :status
                                   :as           pos}]
                               (if-let [price-close (and (#{:open} status)
                                                         (get prices price-hid))]
                                 (assoc pos :close-price price-close)
                                 pos))))]
     (assoc-in db data/positions positions))))
