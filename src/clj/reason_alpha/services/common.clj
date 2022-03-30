(ns reason-alpha.services.common
  (:require [taoensso.timbre :as timbre :refer (errorf)]
            [reason-alpha.model.utils :as mutils]))

(defn delete-fn [fn-repo-delete! model-type]
  (fn [ids]
      (try
        {:result (fn-repo-delete! ids)
         :type   :success}
        (catch Exception e
          (let [err-msg (str "Error deleting " model-type)]
            (errorf e err-msg)
            {:error       (ex-data e)
             :description (str err-msg ": " (ex-message e))
             :type        :error})))))

(defn delete-msg-fn
  [{:keys [fn-repo-delete! model-type fn-get-ctx response-msg-event]}]
  (fn [ids]
    (let [{:keys [send-message]} (fn-get-ctx)]
      (try
          (send-message
           [response-msg-event {:result (fn-repo-delete! ids)
                                :type   :success}])
        (catch Exception e
          (let [err-msg (str "Error deleting " model-type)]
            (errorf e err-msg)
            (send-message
             [response-msg-event {:error       (ex-data e)
                                  :description (str err-msg ": " (ex-message e))
                                  :type        :error}])))))))

(defn save-msg-fn
  [{:keys [fn-repo-save! fn-get-account fn-get-ctx model-type response-msg-event]}]
  (fn [entity]
    (let [model-type-nm          (name model-type)
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
        (send-message
         [response-msg-event
          {:result (-> ent
                       fn-repo-save!
                       (select-keys [creation-id-k id-k]))
           :type   :success}])
        (catch Exception e
          (let [err-msg "Error saving Instrument"]
            (errorf e err-msg)
            (send-message
             [response-msg-event
              {:error       (ex-data e)
               :description (str err-msg ": " (ex-message e))
               :type        :error}])))))))

(defn getn-msg-fn
  [{:keys [fn-repo-getn fn-get-account fn-get-ctx response-msg-event]}]
  (fn [_]
    (let [{acc-id :account/id}   (fn-get-account)
          {:keys [send-message]} (fn-get-ctx)
          instrs                 (fn-repo-getn acc-id)]
      (send-message
       [response-msg-event {:result instrs
                            :type   :success}]))))

(defn get1-msg-fn
  [{:keys [fn-repo-get1 fn-get-ctx response-msg-event]}]
  (fn [id]
    (let [{:keys [send-message]} (fn-get-ctx)
          entity                 (fn-repo-get1 id)]
      (send-message
       [response-msg-event {:result entity
                            :type   :success}]))))
