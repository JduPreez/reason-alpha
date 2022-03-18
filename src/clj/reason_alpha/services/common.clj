(ns reason-alpha.services.common
  (:require [taoensso.timbre :as timbre :refer (errorf)]))

(defn delete-fn [fn-repo-delete! fn-get-account model-type]
  (fn [ids]
    (let [{acc-id :account/id} (fn-get-account ids)]
      (try
        (if acc-id
          {:result (fn-repo-delete! acc-id ids)
           :type   :success}
          {:description "No account found."
           :type        :error})
        (catch Exception e
          (let [err-msg (str "Error deleting " model-type)]
            (errorf e err-msg)
            {:error       (ex-data e)
             :description (str err-msg ": " (ex-message e))
             :type        :error}))))))

(defn delete-msg-fn
  [{:keys [fn-repo-delete! fn-get-account model-type fn-get-ctx response-msg-event]}]
  (fn [ids]
    (let [{acc-id :account/id}   (fn-get-account ids)
          {:keys [send-message]} (fn-get-ctx ids)]
      (try
        (if acc-id
          (send-message
           [response-msg-event {:result (fn-repo-delete! acc-id ids)
                                :type   :success}])
          (send-message
           [response-msg-event {:description "No account found."
                                :type        :error}]))
        (catch Exception e
          (let [err-msg (str "Error deleting " model-type)]
            (errorf e err-msg)
            (send-message
             [response-msg-event {:error       (ex-data e)
                                  :description (str err-msg ": " (ex-message e))
                                  :type        :error}])))))))
