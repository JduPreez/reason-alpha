(ns reason-alpha.events
  (:require [day8.re-frame.tracing :refer-macros [fn-traced]]
            [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.service-api :as svc-api]))

(rf/reg-event-fx
 :navigate
 (fn [{:keys [db]} [_ {{:keys [name]} :data}]]
   (let [db-out (assoc db
                       :active-view-model
                       (get-in db [:view-models name]))]
     (case name
       :trade-patterns {:db       db-out
                        :dispatch [:get-trade-patterns]}
       {:db db-out}))))

(rf/reg-event-db
 :save-local
 (fn-traced
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
         (assoc :saved new-val)))))

(rf/reg-event-fx
 :save-remote
 (fn-traced [_ [_ type entity]]
   (let [command (svc-api/entity-action->http-request
                  {:entities-type type
                   :action        :save
                   :data          entity})]
     command)))

(rf/reg-event-fx
 :save
 (fn [_ [_ type entity]]
   {:dispatch-n [[:save-local type entity]
                 [:save-remote type entity]]}))

(defn action-event
  "Derives the correct toolbar data event-fx from the current
  active model, and dispatches it."
  [{:keys [db]} [action]]
  (let [{:keys [model]} (get-in db data/active-view-model)]
    (when model
      {:dispatch [(keyword (str (name model)
                                "/"
                                (name action)))]})))

(rf/reg-event-fx
 :api-request-failure
 (fn [_ [_ type action error-response]]
   (cljs.pprint/pprint {:api-request-failure
                       {:type           type
                        :action         action
                        :error-response error-response}})))

(rf/reg-event-fx
 :delete-local
 data/delete-local)

(rf/reg-event-fx
 :delete
 action-event)

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
