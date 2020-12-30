(ns reason-alpha.events
  (:require [re-frame.core :as rf]
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
   (let [updated-route-db (assoc db :active-view name)]
     (case name
       :trade-patterns {:db       updated-route-db
                        :dispatch [:get-trade-patterns]}
       {:db updated-route-db}))))

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
         (assoc-in [:data type] merged-coll)))))

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

(rf/reg-event-fx
 :add
 (fn [{:keys [db]} _]
   (let [active-type (get-in db [:active-view :type])]
     {:dispatch [(keyword (str (name active-type)
                               "/add"))]})))

