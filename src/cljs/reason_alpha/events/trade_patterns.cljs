(ns reason-alpha.events.trade-patterns
  (:require [re-frame.core :as rf]
            [reason-alpha.data.trade-patterns :as tp-data]
            [reason-alpha.data :as data]
            [reason-alpha.web.service-api :as svc-api]
            [reason-alpha.utils :as utils]))

(rf/reg-event-db
 :trade-patterns/add
 (fn [db _]
   (update-in db
              tp-data/root
              (fn [trd-patrns]
                (let [default-already-added?          (some #(= "-" (:trade-pattern/name %)) trd-patrns)
                      {parent-id :trade-pattern/id
                       tp-name   :trade-pattern/name} (get-in db data/selected)
                      ancestors-path                  (keep identity [tp-name "-"])
                      x                               (if default-already-added?
                                                        trd-patrns
                                                        (conj trd-patrns
                                                              {:trade-pattern/creation-id    (utils/new-uuid)
                                                               :trade-pattern/name           "-"
                                                               :trade-pattern/description    ""
                                                               :trade-pattern/ancestors-path ancestors-path
                                                               :trade-pattern/parent-id      parent-id}))]
                  (cljs.pprint/pprint {::add x})
                  x)))))

(rf/reg-event-fx
 :get-trade-patterns
 (fn [{:keys [db]} [_ params]]
   {:http-xhrio (svc-api/http-request :get :trade-patterns db params)
    :db         (-> db
                    (assoc-in [:loading :trade-patterns] true))}))

(comment
  {:method          :get
   :uri             (endpoint "trade-patterns")
   :params          params
   :headers         (standard-headers db)
   :format          (ajax/transit-request-format)
   :response-format (ajax/transit-response-format)
   :on-success      [:save-local :trade-patterns]})
