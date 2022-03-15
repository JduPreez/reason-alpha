(ns reason-alpha.data
  (:require [reason-alpha.model.utils :as model.utils]
            [reason-alpha.web.api-client :as api-client]))

(def ^:const selected [:selected])

(def ^:const active-view-model [:active-view-model])

(def ^:const api-info [:data :api])

(def ^:const trade-patterns [:data :trade-pattern])

(def ^:const holdings [:data :holding])

(def ^:const models [:data :model])

(defn model [model-k]
  (conj models model-k))

(def ^:const instruments [:data :instrument])

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
        id-k           (model.utils/id-key type (first del-col))
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
        creation-id-k       (model.utils/creation-id-key-by-type type)
        id-k                (model.utils/id-key-by-type type)
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
    ids))

(defn save-local!
  [{db :db, type :model-type, data :data}]
  (let [current     (get-in db (entity-data type))
        new-val     (or (:result data) data)
        new-coll    (cond
                      (and (coll? new-val)
                           (not (map? new-val))
                           (map? (first new-val))) new-val
                      (map? new-val)               [new-val])
        merged-coll (when new-coll
                      (model.utils/merge-by-id type current new-coll))]
    (-> db
        (assoc-in [:loading type] false)
        (assoc-in [:data type] (or merged-coll new-val))
        (assoc :saved new-val))))

(defn save-remote! [{:keys [command data success-event]}]
  (api-client/chsk-send! [command data] {:on-success success-event}))

(defn save-event-fn [type success]
  (fn [_ [_ entity]]
    (let [cmd (-> type
                  name
                  (str ".command/save!")
                  keyword)]
      {:save-remote [cmd entity success]
       :dispatch    [:save-local type entity]})))
