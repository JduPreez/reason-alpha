(ns reason-alpha.events
  (:require [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.model.utils :as model.utils]
            [reason-alpha.utils :as utils]
            ;; TODO: Maybe improve this? Not sure I like that the `events`
            ;; ns references `views` directly ...
            [reason-alpha.views :as views]
            [reason-alpha.web.api-client :as api-client]
            [reitit.core :as r]
            [reitit.frontend.controllers :as rfe-ctrls]
            [reitit.frontend.easy :as rfe-easy]
            [clojure.string :as str]))

(rf/reg-event-fx
 :initialize-db
 (fn [{:keys [db]} _]
   (if db
     {:db       db
      :dispatch [:init-router]}
     {:db       {:current-route nil}
      :dispatch [:init-router]})))

(rf/reg-event-db
 :init-router
 (fn [db [_]]
   (assoc-in db data/router views/router)))

(rf/reg-event-fx
 :close-active-form
 (fn [{:keys [db]} [_]]
   (let [{{:keys [name]} :sheet-view
          :as            vm} (-> db
                                 (get-in data/active-view-model)
                                 (select-keys [:sheet-view]))
         db                  (assoc-in db data/active-view-model vm)]
     {:db       db
      :dispatch [:push-state name]})))

(rf/reg-event-fx
 :navigated
 (fn [{:keys [db]} [_ {{:keys [form]} :query-params
                       {:keys [name model
                               view load-event
                               load-fx]
                        :as   d}      :data
                       :as            new-match} router]]
   (cljs.pprint/pprint {::navigated new-match})
   (let [{{frm-name       :name
           frm-model      :model
           frm-view       :view
           frm-load-event :load-event} :data} (when form
                                                (r/match-by-path router (str "/forms/" form)))
         ;; TODO: Check if new-match and old-match is the same, if so don't do anything???
         old-match                            (:current-route db)
         controllers                          (rfe-ctrls/apply-controllers (:controllers old-match)
                                                                           new-match)
         view-model                           (cond-> {:sheet-view {:name  name
                                                                    :model model
                                                                    :view view}}
                                                frm-name (assoc :form-view {:name  frm-name
                                                                            :model frm-model
                                                                            :view  frm-view}))
         updated-db                           (-> db
                                                  (assoc :current-route (assoc new-match
                                                                               :controllers controllers))
                                                  (assoc-in data/active-view-model view-model))
         efx                                  (cond-> {:db updated-db}
                                                load-fx          (assoc load-fx [])
                                                load-event       (assoc :dispatch-n [[:datagrid/update-history name]
                                                                                     load-event])
                                                frm-load-event   (update :dispatch-n #(if (seq %)
                                                                                        (conj % frm-load-event)
                                                                                        [frm-load-event]))
                                                (not load-event) (assoc :dispatch [:datagrid/update-history name]))]
     (cljs.pprint/pprint {::navigated-2 efx})
     efx)))

(rf/reg-fx
 :push-state!
 (fn [route]
   (apply rfe-easy/push-state route)))

(rf/reg-event-fx
 :push-state
 (fn [_ [_ & route]]
   {:push-state! route}))

(rf/reg-event-fx
 :push-state/active-form
 (fn [{:keys [db]} [_ form]]
   (let [{{r :name} :data} (get-in db data/current-route)
         router            (get-in db data/router)
         f                 (-> router
                               (r/match-by-name form)
                               r/match->path
                               (str/replace #"/forms/" ""))]
     {:dispatch [:push-state r nil {:form f}]})))

(defn entity-event-or-fx-key [db action]
  (let [{:keys [model]} (get-in db data/active-view-model)]
    (when model
      (-> model
          name
          (keyword (name action))))))

(defn-traced action-event
  "Derives the correct toolbar data event-fx from the current
  active model, and dispatches it."
  [{:keys [db]} [action]]
  (let [{:keys [model]} (get-in db data/active-view-model)
        _               (cljs.pprint/pprint {::action-event-1 model})
        event           [(keyword (name model) (name action))]]
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
   (let [{{:keys [name]} :sheet-view} (get-in db data/active-view-model)]
     {:dispatch [:datagrid/create-new-record name]})))

(rf/reg-event-fx
 :cancel
 (fn [{:keys [db]} _]
   {:dispatch [:datagrid/cancel-editing]}))

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

(rf/reg-event-db
 :view.data/update
 (fn [db [_ view field value]]
   (data/update-view-data db view field value)))

(rf/reg-event-db
 :view.data/init
 (fn [db [_ view model-type entity]]
   (data/init-view-data db view model-type entity)))

(rf/reg-event-fx
 :view.data/save
 (fn [{:keys [db]} [_ view save-event-fx]]
   (data/save-view-data db view save-event-fx)))
