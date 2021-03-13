(ns reason-alpha.events.trade-patterns
  (:require [re-frame.core :as rf]
            [reason-alpha.data.trade-patterns :as data]
            [reason-alpha.web.service-api :as svc-api]
            [reason-alpha.utils :as utils]))

(rf/reg-event-db
 :trade-patterns/add
 (fn [db _]
   (update-in db
              data/root
              #(conj % (utils/new-entity :trade-pattern)))))

(rf/reg-event-fx
 :get-trade-patterns
 (fn [{:keys [db]} [_ params]]
   {:http-xhrio (svc-api/http-request :get :trade-patterns db params)
    :db         (-> db
                    (assoc-in [:loading :trade-patterns] true))}))

(comment
  {:method            :get
   :uri             (endpoint "trade-patterns")
   :params          params
   :headers         (standard-headers db)
   :format          (ajax/transit-request-format)
   :response-format (ajax/transit-response-format)
   :on-success      [:save-local :trade-patterns]})
