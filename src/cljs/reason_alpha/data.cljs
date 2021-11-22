(ns reason-alpha.data
  (:require [reason-alpha.utils :as utils]
            [reason-alpha.web.service-api :as svc-api]))

(def ^:const selected [:selected])

(def ^:const active-view-model [:active-view-model])

(def ^:const api-info [:data :api])

(defn entity-data [entities-type]
  [:data entities-type])

(defn delete-local
  [{:keys [db]} [_ entities-type {{:keys [deleted-items]} :result
                                  :as                     deleted}]]
  (let [data-path      (entity-data entities-type)
        deleted        (or deleted-items deleted)
        del-col        (if (coll? deleted)
                         deleted
                         [deleted])
        id-k           (utils/id-key (first del-col))
        entities       (get-in db data-path)
        remaining-ents (remove
                        (fn [e]
                          (let [id-v (get e id-k)]
                            (some #(let [del-id-v (get % id-k)]
                                     (= del-id-v id-v)) deleted)))
                        entities)]
    {:db       (assoc-in db data-path remaining-ents)
     :dispatch [:select nil]}))

(defn delete [entities-type db]
  (let [ids      (get-in db selected entities-type)
        http-req (svc-api/entity-action->http-request
                  {:entities-type entities-type
                   :action        :delete
                   :data          ids
                   :on-success    [:delete-local entities-type]})]
    http-req))
