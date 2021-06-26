(ns reason-alpha.events
  (:require [medley.core :refer [dissoc-in]]
            [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.service-api :as svc-api]))

#_(defn- standard-headers
  "Adds:
    * The user token and format for API authorization
    * CSRF token"
  [db]
  (let [headers [:x-csrf-token js/csrfToken]]
    (when-let [token (get-in db [:user :token])]
      (conj headers :Authorization (str "Token " token)))))

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
     (cljs.pprint/pprint {::save-local {:type    type
                                        :current current
                                        :new     new
                                        :merged  merged-coll}})
     (-> db
         (assoc-in [:loading type] false)
         (assoc-in [:data type] merged-coll)
         (assoc :saved new-val)))))

;; TODO: Make sure save-remote uses a collection arg
(rf/reg-event-fx
 :save-remote
 (fn [_ [_ type entity]]
   (cljs.pprint/pprint {::save-remote entity})
   (let [command (svc-api/entity->command [type entity])]
     command)))

(rf/reg-event-fx
 :save
 (fn [_ [_ type entity]]
   {:dispatch-n [[:save-local type entity]
                 [:save-remote type entity]]}))

(defn add
  "Derives the correct add event-fx from the current active model, and
  dispatches it."
  [{:keys [db]} _]
  (let [{:keys [model]} (get-in db [:active-view-model])]
    (when model
      {:dispatch [(keyword (str (name model)
                                "/add"))]})))

(rf/reg-event-fx
 :add
 add)

(rf/reg-event-db
 :select
 (fn [db [_ entity]]
   (cljs.pprint/pprint {::select entity})
   (if (seq entity)
     (assoc-in db data/selected entity)
     (dissoc-in db data/selected))))

(rf/reg-event-db
 :set-view-models
 (fn [db [_ view-models]]
   (assoc db :view-models view-models)))

