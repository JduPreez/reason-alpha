(ns reason-alpha.events
  (:require [medley.core :refer [dissoc-in]]
            [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.service-api :as svc-api]))

(rf/reg-event-fx
 :get-api-info
 (fn [{:keys [db]} _]
   (svc-api/entity-action->http-request db :api :get)))

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
 (fn [db [_ type {:keys [result] :as new}]]
   (let [current     (get-in db [:data type])
         new-val     (or result new)
         new-coll    (cond
                       (and (coll? new-val)
                            (not (map? new-val))) new-val
                       (map? new-val)             [new-val]
                       :else                      new-val)
         merged-coll (utils/merge-by-id current new-coll)]
     (-> db
         (assoc-in [:loading type] false)
         (assoc-in [:data type] merged-coll)
         (assoc :saved new-val)))))

;; TODO: Make sure save-remote uses a collection arg
(rf/reg-event-fx
 :save-remote
 (fn [_ [_ type entity]]
   (let [command (svc-api/entity->command [type entity])]
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
  (cljs.pprint/pprint {::action-event action})
  (let [{:keys [model]} (get-in db data/active-view-model)]
    (when model
      {:dispatch [(keyword (str (name model)
                                "/"
                                (name action)))]})))

(rf/reg-event-fx
 :add
 action-event)

(rf/reg-event-fx
 :delete
 action-event)

(rf/reg-event-fx
 :cancel
 action-event)

(rf/reg-event-db
 :select
 (fn [db [_ entity]]
   (if (seq entity)
     (assoc-in db data/selected entity)
     (dissoc-in db data/selected))))

(rf/reg-event-db
 :set-view-models
 (fn [db [_ view-models]]
   (assoc db :view-models view-models)))
