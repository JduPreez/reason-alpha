(ns reason-alpha.events.trade-patterns
  (:require [re-frame.core :as rf]
            [reason-alpha.data.trade-patterns :as tp-data]
            [reason-alpha.data :as data]
            [reason-alpha.web.service-api :as svc-api]
            [reason-alpha.web.api-client :as api-client]
            [reason-alpha.utils :as utils]
            [reason-alpha.events :as events]))

(rf/reg-event-db
 :trade-patterns/add
 (fn [db _]
   (update-in db
              tp-data/root
              (fn [trd-patrns]
                (let [selected-ids   (get-in db data/selected)
                      selected-ents  (filter #(and (some #{(get % :trade-pattern/id)} selected-ids)
                                                   (not (:trade-pattern/parent-id %)))
                                             trd-patrns)
                      new-trd-patrns (if (seq selected-ents)
                                       (mapv (fn [tp]
                                               {:trade-pattern/id          (utils/new-uuid)
                                                :trade-pattern/parent-id   (:trade-pattern/id tp)
                                                :trade-pattern/name        ""
                                                :trade-pattern/description ""})
                                             selected-ents)
                                       [{:trade-pattern/id          (utils/new-uuid)
                                         :trade-pattern/name        ""
                                         :trade-pattern/description ""}])]
                  (cljs.pprint/pprint {:trade-patterns/add new-trd-patrns})
                  (into trd-patrns new-trd-patrns))))))

(rf/reg-event-fx
 :trade-pattern.command/create
 (fn [_ [_ new-trade-pattern]]
   {:dispatch [:trade-pattern.command/save!
               (assoc new-trade-pattern
                      :trade-pattern/creation-id
                      (utils/new-uuid))]}))

(rf/reg-event-fx
 :trade-pattern.command/save!
 (events/fn-save :trade-pattern [:trade-pattern/success]))

(rf/reg-fx
 :trade-pattern.command/delete!
 (fn [db]
   (let [del-ids (data/get-deleted-ids :trade-pattern db)]
     (api-client/chsk-send! [:trade-pattern.command/delete! del-ids]
                            {:on-success [:trade-pattern/delete-success
                                          :trade-pattern]}))))

(rf/reg-event-fx
 :trade-pattern/delete-success
 data/delete-local!)

(rf/reg-event-fx
 :trade-pattern/success
 (fn [_ [_ result]]
   (cljs.pprint/pprint {:trade-pattern/success result})
   {:dispatch [:save-local :trade-pattern result]}))

(rf/reg-fx
 :trade-pattern.query/get
 (fn [_]
   (cljs.pprint/pprint {:trade-pattern.query/get _})
   (api-client/chsk-send! [:trade-pattern.query/get] {:on-success [:trade-pattern/success]})))

(comment
  {:method          :get
   :uri             (endpoint "trade-patterns")
   :params          params
   :headers         (standard-headers db)
   :format          (ajax/transit-request-format)
   :response-format (ajax/transit-response-format)
   :on-success      [:save-local :trade-patterns]})
