(ns reason-alpha.events
  (:require [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.api-client :as api-client]
            [reason-alpha.web.service-api :as svc-api]))

(rf/reg-event-fx
 :navigate
 (fn [{{:keys [view-models]
        :as   db} :db} [_ {{vid :name} :data}]]
   (let [db-out (->> {:view-id vid}
                     (merge (get view-models vid))
                     (assoc-in db data/active-view-model))]
     (case vid
       :trade-patterns {:db                      db-out
                        :trade-pattern.query/get []}
       {:db db-out}))))

(defn-traced save-local
  [db [_ type {:keys [result] :as new}]]
  (let [current     (get-in db (data/entity-data type))
        new-val     (or result new)
        new-coll    (cond
                      (and (coll? new-val)
                           (not (map? new-val))
                           (map? (first new-val))) new-val
                      (map? new-val)               [new-val])
        merged-coll (when new-coll
                      (utils/merge-by-id current new-coll))]
    (-> db
        (assoc-in [:loading type] false)
        (assoc-in [:data type] (or merged-coll new-val))
        (assoc :saved new-val))))

(rf/reg-event-db
 :save-local
 save-local)

(defn save-remote [[command entity success]]
  (api-client/chsk-send! [command entity] {:on-success success}))

(rf/reg-fx
 :save-remote
 save-remote)

(defn fn-save [type success]
  (fn [_ [_ entity]]
    (let [cmd (-> type
                  name
                  (str ".command/save!")
                  keyword)]
      {:save-remote [cmd entity success]
       :dispatch    [:save-local type entity]})))

#_(rf/reg-event-fx
 :save
 (fn [_ [_ type entity]]
   {:dispatch-n [[:save-local type entity]
                 [:save-remote type entity]]}))

(defn entity-event-or-fx-key [db action]
  (let [{:keys [model]} (get-in db data/active-view-model)]
    (when model
      (-> model
          name
          (str ".command/"
               (name action))
          keyword))))

(defn-traced action-event
  "Derives the correct toolbar data event-fx from the current
  active model, and dispatches it."
  [{:keys [db]} [action]]
  (let [{:keys [model]} (get-in db data/active-view-model)
        event           [(keyword (str (name model)
                                       ".command/"
                                       (name action)))]]
    (cljs.pprint/pprint {::action-event [action event]})
    (if model
      {:dispatch event}
      {})))

(rf/reg-event-fx
 :api-request-failure
 (fn [_ [_ type action error-response]]
   (cljs.pprint/pprint {:api-request-failure
                       {:type           type
                        :action         action
                        :error-response error-response}})))

(rf/reg-event-fx
 :delete!
 (fn [{:keys [db]} [action]]
   (let [fx (entity-event-or-fx-key db action)]
     {fx db})))

(rf/reg-event-fx
 :add
 action-event)

(rf/reg-event-fx
 :cancel
 action-event)

(rf/reg-event-fx
 :create
 action-event)

(rf/reg-event-db
 :select
 (fn [db [_ selected-ids]]
   (let [{:keys [model]} (get-in db data/active-view-model)
         ids             (map #(utils/maybe->uuid %) selected-ids)]
     (assoc-in db (conj data/selected model) ids))))

(rf/reg-event-db
 :set-view-models
 (fn [db [_ view-models]]
   (assoc db :view-models view-models)))

(rf/reg-event-fx
 :edit
 (fn [{:keys [db]} [_ entities]]
   (let [{:keys [view-id]} (get-in db data/active-view-model)]
     {:dispatch-n (mapv #(let [creation-id (->> %
                                                utils/creation-id-key
                                                (get %))]
                           [:datagrid/start-edit view-id creation-id %])
                        entities)})))
