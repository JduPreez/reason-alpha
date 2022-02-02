(ns reason-alpha.data
  (:require [reason-alpha.utils :as utils]))

(def ^:const selected [:selected])

(def ^:const active-view-model [:active-view-model])

(def ^:const api-info [:data :api])

(defn entity-data [type]
  [:data type])

(defn delete-local!
  [{:keys [db]} [_ type {{:keys [deleted-items]} :result
                         :as                     deleted}]]
  (let [data-path      (entity-data type)
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

#_(defn delete-local!
  [{:keys [db]} [_ type {{:keys [deleted-items]} :result
                         :as                     deleted}]]
  (let [data-path      (entity-data type)
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

(defn get-selected-ids [type db]
  (let [selctd-creation-ids (->> type
                                 (conj selected)
                                 (get-in db))
        creation-id-k       (utils/creation-id-key-by-type type)
        id-k                (utils/id-key-by-type type)
        idx-ents            (->> type
                                 entity-data
                                 (get-in db)
                                 (reduce (fn [idx-ents' ent]
                                           (assoc idx-ents' (get ent creation-id-k) ent))
                                         {}))
        ids                 (->> selctd-creation-ids
                                 (map (fn [cid]
                                        (-> idx-ents
                                            (get cid)
                                            (get id-k)))))]
    (cljs.pprint/pprint {::get-selected-ids [ids]})
   ids))

(comment
  (get-deleted-ids :trade-pattern @db')

  (reduce (fn [idx-m {:keys [x] :as m}]
            (assoc idx-m x m))
          {}
          [{:x "1"}
           {:x "2"}
           {:x "3"}])

  )

#_(defn delete! [type db]
  (let [ids      (->> type
                      (conj selected)
                      (get-in db))
        http-req (svc-api/entity-action->http-request
                  {:entities-type entities-type
                   :action        :delete
                   :data          ids
                   :on-success    [:delete-local! entities-type]})]
    http-req))
