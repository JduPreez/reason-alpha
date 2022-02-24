(ns reason-alpha.events.models
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.web.api-client :as api-client]
            [reason-alpha.utils :as utils]
            [reason-alpha.events :as events]
            [reason-alpha.data :as data]))

(rf/reg-event-db
 :model.query/getn-response
 (fn [db [_ {:keys [result]}]]
   (let [models (get-in db data/models {})]
     (assoc-in db data/models (merge models result)))))
