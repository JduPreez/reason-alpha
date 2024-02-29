(ns reason-alpha.services.common
  (:require [clojure.set :as set]
            [reason-alpha.model.core :as model]
            [reason-alpha.model.utils :as mutils]
            [reason-alpha.utils :as utils]
            [taoensso.timbre :as timbre :refer (errorf)]))

(defn delete-fn
  [{:keys [fn-repo-delete! model-type fn-get-referenced-ids]}]
  (fn [ids]
    (try
      (let [refed-ids (when fn-get-referenced-ids
                        (->> ids
                             fn-get-referenced-ids
                             set))
            ids       (if (seq refed-ids)
                        (set/difference (set ids) refed-ids)
                        ids)
            deleted   (when (seq ids) (fn-repo-delete! ids))]
        {:result-id (utils/new-uuid)
         :result    {:deleted            deleted
                     :referenced-not-del refed-ids}
         :type      :success})
      (catch Exception e
        (let [err-msg (str "Error deleting " model-type)]
          (errorf e err-msg)
          {:result-id   (utils/new-uuid)
           :error       (ex-data e)
           :description (str err-msg ": " (ex-message e))
           :type        :error})))))

(defn delete-msg-fn
  [{:keys [fn-repo-delete! model-type fn-get-ctx response-msg-event
           fn-get-referenced-ids]}]
  (fn [ids]
    (println ::delete-msg-fn)
    (let [{:keys [send-message]} (fn-get-ctx)]
      (try
        (let [refed-ids (when fn-get-referenced-ids
                          (->> ids
                               fn-get-referenced-ids
                               set))
              ids       (if (seq refed-ids)
                          (set/difference (set ids) refed-ids)
                          ids)
              deleted   (when (seq ids) (fn-repo-delete! ids))]
          (send-message
           [response-msg-event {:result-id (utils/new-uuid)
                                :result    {:deleted            deleted
                                            :referenced-not-del refed-ids}
                                :type      :success}]))
        (catch Exception e
          (let [err-msg (str "Error deleting " model-type)]
            (errorf e err-msg)
            (send-message
             [response-msg-event {:result-id   (utils/new-uuid)
                                  :error       (ex-data e)
                                  :description (str err-msg ": " (ex-message e))
                                  :type        :error}])))))))

(defn save-msg-fn
  [{:keys [fn-repo-save! fn-get-account fn-get-ctx schema-k response-msg-event]}]
  (fn [entity]
    (let [model-type-nm          (name schema-k)
          acc-id-k               (keyword model-type-nm "account-id")
          creation-id-k          (keyword model-type-nm "creation-id")
          id-k                   (keyword model-type-nm "id")
          acc-id                 (acc-id-k entity)
          ent                    (if acc-id
                                   entity
                                   (->> (fn-get-account)
                                        :account/id
                                        (assoc entity acc-id-k)))
          {:keys [send-message]} (fn-get-ctx)]
      (try
        (if-let [v (model/validate schema-k ent)]
          (send-message
           [response-msg-event
            {:error       v
             :type        :failed-validation
             :description (format "Invalid '%s'" model-type-nm)}])
          (send-message
           [response-msg-event
            {:result (-> ent
                         fn-repo-save!
                         (select-keys [creation-id-k id-k]))
             :type   :success}]))
        (catch Exception e
          (let [err-msg (format "Error saving '%s'" model-type-nm)]
            (errorf e err-msg)
            (send-message
             [response-msg-event
              {:error       (ex-data e)
               :description (str err-msg ": " (ex-message e))
               :type        :error}])))))))

(comment
  (model/validate :model/position #:position{:creation-id
                                             #uuid "35ccc174-5563-480b-b51e-28bd27bdd396",
                                             :holding-id
                                             #uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5",
                                             :open
                                             #:trade-transaction{:quantity
                                                                 "45",
                                                                 :date
                                                                 #inst "2022-05-01T00:00:00.000-00:00",
                                                                 :price
                                                                 "67.8"},
                                             :long-short :long} )
  (get @model/*model :model/position)
  (model/get-def :model/position)

  )

(defn getn-msg-fn
  [{:keys [fn-repo-getn fn-get-account fn-get-ctx response-msg-event]}]
  (fn [_]
    (let [{acc-id :account/id}   (fn-get-account)
          {:keys [send-message]} (fn-get-ctx)
          ents                   (fn-repo-getn acc-id)]
      (clojure.pprint/pprint {::getn-msg-fn [response-msg-event ents]})
      (send-message
       [response-msg-event {:result ents
                            :type   :success}]))))

(defn get1-msg-fn
  [{:keys [fn-repo-get1 fn-get-ctx response-msg-event]}]
  (fn [id]
    (let [{:keys [send-message]} (fn-get-ctx)
          entity                 (fn-repo-get1 id)]
      (send-message
       [response-msg-event {:result entity
                            :type   :success}]))))
