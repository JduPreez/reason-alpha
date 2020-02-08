(ns reason-alpha.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]))

;;dispatchers

(rf/reg-event-db
 :navigate
 (fn [db [_ route]]
   (assoc db :route route)))

(rf/reg-event-db
 :set-docs
 (fn [db [_ docs]]
   (assoc db :docs docs)))

(rf/reg-event-fx
 :fetch-docs
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/docs"
                 :response-format (ajax/raw-response-format)
                 :on-success      [:set-docs]}}))

(rf/reg-event-db
 :common/set-error
 (fn [db [_ error]]
   (assoc db :common/error error)))

;;subscriptions

(rf/reg-sub
 :route
 (fn [db _]
   (-> db :route)))

(rf/reg-sub
 :page
 :<- [:route]
 (fn [route _]
   (-> route :data :name)))

(rf/reg-sub
 :docs
 (fn [db _]
   (:docs db)))

(rf/reg-sub
 :common/error
 (fn [db _]
   (:common/error db)))

(comment
  (ns reason-alpha.events
    (:require [clojure.string :as str]
              [day8.re-frame.tracing :refer-macros [fn-traced]]
              [re-frame.core :as rf]
              [ajax.core :as ajax])))

(def api-url "/api")

(defn- endpoint 
  "Concat any params to api-url separated by /"
  [& params]
  (str/join "/" (concat [api-url] params)))

(defn- standard-headers 
  "Adds:
    * The user token and format for API authorization
    * CSRF token"
  [db]
  (let [headers [:x-csrf-token js/csrfToken]]
    (when-let [token (get-in db [:user :token])]
      (conj headers :Authorization (str "Token " token)))))

;; Dispatchers

;;; Navigation
(rf/reg-event-db
 :navigate
 (fn [db [_ route]]
   (assoc db :route route)))

;;; Trade Patterns
(rf/reg-event-fx
 :get-trade-patterns->
 (fn-traced [{:keys [db]} [_ params]]
            {:http-xhrio {:method     :get
                          :uri        (endpoint "trade-patterns")
                          :params     params
                          :headers    (standard-headers db)
                          :on-success [:store-trade-patterns]}
             :db (-> db
                     (assoc-in [:loading :trade-patterns] true))}))

(rf/reg-event-db
 :store-trade-patterns
 (fn-traced [db [_ trade-patterns]]
            (-> db
                (assoc-in [:loading :trade-patterns] false)
                (assoc :trade-patterns trade-patterns))))

;;; Documents
(rf/reg-event-db
 :set-docs
 (fn [db [_ docs]]
   (assoc db :docs docs)))

(rf/reg-event-fx
 :fetch-docs
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/docs"
                 :response-format (ajax/raw-response-format)
                 :on-success      [:set-docs]}}))

;;; Common
(rf/reg-event-db
 :common/set-error
 (fn [db [_ error]]
   (assoc db :common/error error)))

#_(reg-event-db
 :initialise-db
 (fn [_ _]
   default-db))

;; Subscriptions

(rf/reg-sub
 :route
 (fn [db _]
   (-> db :route)))

(rf/reg-sub
 :page
 :<- [:route]
 (fn [route _]
   (-> route :data :name)))

(rf/reg-sub
 :docs
 (fn [db _]
   (:docs db)))

(rf/reg-sub
 :common/error
 (fn [db _]
   (:common/error db)))
