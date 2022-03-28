(ns reason-alpha.services.common
  (:require [taoensso.timbre :as timbre :refer (errorf)]))

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
