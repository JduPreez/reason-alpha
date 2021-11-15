(ns reason-alpha.events.trade-patterns
  (:require [re-frame.core :as rf]
            [reason-alpha.data.trade-patterns :as tp-data]
            [reason-alpha.data :as data]
            [reason-alpha.web.service-api :as svc-api]
            [reason-alpha.utils :as utils]))

;; TODO: Old, remove once new version works
#_(rf/reg-event-db
 :trade-patterns/add
 (fn [db _]
   (update-in db
              tp-data/root
              (fn [trd-patrns]
                (let [default-already-added?     (some #(= "-" (:trade-pattern/name %)) trd-patrns)
                      {parent-id
                       :trade-pattern/id
                       parent-name
                       :trade-pattern/name
                       parents-parent-id
                       :trade-pattern/parent-id} (get-in db data/selected)
                      ;; At this stage only 1 level deep tree is supported, so make sure
                      ;; the selected row isn't a child row already.
                      top-level-parent?          (nil? parents-parent-id)
                      ancestors-path             (keep identity [(when top-level-parent?
                                                                   parent-name)
                                                                 "-"])
                      updated-trd-patrns         (if default-already-added?
                                                   trd-patrns
                                                   (conj trd-patrns
                                                         {:trade-pattern/creation-id    (utils/new-uuid)
                                                          :trade-pattern/name           "-"
                                                          :trade-pattern/description    ""
                                                          :trade-pattern/ancestors-path ancestors-path
                                                          :trade-pattern/parent-id      (when top-level-parent?
                                                                                          parent-id)}))]
                  updated-trd-patrns)))))



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
 :trade-patterns/create
 (fn [_ [_ new-trade-pattern]]
   {:dispatch [:save :trade-patterns
               (assoc new-trade-pattern
                      :trade-pattern/creation-id
                      (utils/new-uuid))]}))

(rf/reg-event-db
 :trade-patterns/delete
 (fn [db _]
   (cljs.pprint/pprint {:trade-patterns/delete (get-in db data/selected)})
   db))

(rf/reg-event-fx
 :get-trade-patterns
 (fn [{:keys [db]} [_ params]]
   (merge (svc-api/entity-action->http-request
           {:entities-type :trade-patterns
            :action        :get
            :data          params})
          {:db (-> db
                   (assoc-in [:loading :trade-patterns] true))})))

(comment
  {:method          :get
   :uri             (endpoint "trade-patterns")
   :params          params
   :headers         (standard-headers db)
   :format          (ajax/transit-request-format)
   :response-format (ajax/transit-response-format)
   :on-success      [:save-local :trade-patterns]})
