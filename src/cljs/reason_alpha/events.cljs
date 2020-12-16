(ns reason-alpha.events
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.service-api :as svc-api]))

(def api-url "http://localhost:3000/api")

(defn- standard-headers
  "Adds:
    * The user token and format for API authorization
    * CSRF token"
  [db]
  (let [headers [:x-csrf-token js/csrfToken]]
    (when-let [token (get-in db [:user :token])]
      (conj headers :Authorization (str "Token " token)))))

(defn- endpoint
  "Concat any params to api-url separated by /"
  [& params]
  (str/join "/" (concat [api-url] params)))

;; Events
(rf/reg-event-fx
 :navigate
 (fn [{:keys [db] } [_ {{:keys [name]} :data}]]
   #_(js/console.log (str "navigate: " name))
   #_(cljs.pprint/pprint x)
   (let [updated-route-db (assoc db :active-view name)] 
     (case name
       :trade-patterns {:db       updated-route-db
                        :dispatch [:get-trade-patterns]}
       {:db updated-route-db}))))

(rf/reg-event-fx
   :get-trade-patterns
   (fn [{:keys [db]} [_ params]]
     (js/console.log ":get-trade-patterns")
     {:http-xhrio {:method          :get
                   :uri             (endpoint "trade-patterns")
                   :params          params
                   :headers         (standard-headers db)
                   :format          (ajax/transit-request-format)
                   :response-format (ajax/transit-response-format)
                   :on-success      [:save-local :trade-pattern]}
      :db         (-> db
                      (assoc-in [:loading :trade-pattern] true))}))

(rf/reg-event-db
 :save-local
 (fn [db [_ type {:keys [result] :as new}]]
   (cljs.pprint/pprint {::save-local [type new]})
   (let [current     (get-in db [:data type])
         new-val     (or result new)
         new-coll    (cond
                       (and (coll? new-val)
                            (not (map? new-val))) new-val
                       (map? new-val)             [new-val]
                       :else                      new-val)
         merged-coll (utils/merge-by-id type current new-coll)]
     (cljs.pprint/pprint {::save-local {:new-coll    new-coll
                                        :merged-coll merged-coll}})
     (-> db
         (assoc-in [:loading type] false)
         (assoc-in [:data type] merged-coll)))))

;; TODO: Make sure save-remote uses a collection arg
(rf/reg-event-fx
 :save-remote
 (fn [_ [_ type entity]]
   (utils/log ::save-remote type "entity" entity)
   (let [command (svc-api/entity->command [type entity])]
     (utils/log ::save-remote type "command" command)
     command)))

(rf/reg-event-fx
 :save
 (fn [_ [_ type entity]]
   {:dispatch-n [[:save-local type entity]
                 [:save-remote type entity]]}))

;; Subscriptions
(rf/reg-sub
 :active-view           ;; usage: (subscribe [:active-view])
 (fn [db _]             ;; db is the (map) value stored in the app-db atom
   (:active-view db)))  ;; extract a value from the application state


(rf/reg-sub
 :loading
 (fn [db [_ key']]
   {key' (true?
          (get-in db [:loading key']))}))
