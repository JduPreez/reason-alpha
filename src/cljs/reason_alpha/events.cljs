(ns reason-alpha.events
  (:require [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.data :as data]
            [reason-alpha.model.utils :as model.utils]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.api-client :as api-client]
            [reitit.frontend.controllers :as rfe-ctrls]
            [reitit.frontend.easy :as rfe-easy]))

(rf/reg-event-db
 :initialize-db
 (fn [db _]
   (if db
     db
     {:current-route nil})))

(rf/reg-event-fx
 :navigated
 (fn [{:keys [db]} [_ {{:keys [name model
                               load-event
                               load-fx]
                        :as   d} :data
                       :as       new-match}]]
   (let [old-match   (:current-route db)
         controllers (rfe-ctrls/apply-controllers (:controllers old-match) new-match)
         updated-db  (-> db
                         (assoc :current-route (assoc new-match :controllers controllers))
                         (assoc-in data/active-view-model {:view  name
                                                           :model model}))]
     (cond-> {:db updated-db}
       load-fx          (assoc load-fx [])
       load-event       (assoc :dispatch-n [[:datagrid/update-history name]
                                            load-event])
       (not load-event) (assoc :dispatch [:datagrid/update-history name])))))

(rf/reg-fx
 :push-state!
 (fn [route]
   (apply rfe-easy/push-state route)))

(rf/reg-event-fx
 :push-state
 (fn [_ [_ & route]]
   {:push-state! route}))

;; (defn-traced save-local
;;   [db [_ type {:keys [result] :as new}]]
;;   (let [current     (get-in db (data/entity-data type))
;;         new-val     (or result new)
;;         new-coll    (cond
;;                       (and (coll? new-val)
;;                            (not (map? new-val))
;;                            (map? (first new-val))) new-val
;;                       (map? new-val)               [new-val])
;;         merged-coll (when new-coll
;;                       (model.utils/merge-by-id current new-coll))]
;;     (-> db
;;         (assoc-in [:loading type] false)
;;         (assoc-in [:data type] (or merged-coll new-val))
;;         (assoc :saved new-val))))

;; (rf/reg-event-db
;;  :save-local
;;  (defn-traced [db [_ type result]]
;;    (data/save-local! {:db db, :model-type type, :result result})))

;; (rf/reg-fx
;;  :save-remote
;;  save-remote)

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
        _               (cljs.pprint/pprint {::action-event-1 model})
        event           [(keyword (str (name model)
                             ".command/"
                             (name action)))]]
    (cljs.pprint/pprint {::action-event-2 [action event]})
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
 (fn [{:keys [db]} _]
   (let [{:keys [view]} (get-in db data/active-view-model)]
     {:dispatch [:datagrid/create-new-record view]})))

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

(rf/reg-event-fx
 :edit
 (fn [{:keys [db]} [_ entities]]
   (let [{:keys [view model]} (get-in db data/active-view-model)
         edit-evts            (mapv
                               #(let [creation-id (->> %
                                                       (model.utils/creation-id-key model)
                                                       (get %))]
                                  [:datagrid/start-edit view creation-id %])
                               entities)]
     {:dispatch-n edit-evts})))
