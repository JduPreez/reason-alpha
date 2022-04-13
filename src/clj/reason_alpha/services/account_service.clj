(ns reason-alpha.services.account-service
  (:require [medley.core :as medley]))

(defn get-account [fn-get-ctx fn-repo-get-acc-by-uid]
  (let [{{:keys [account/user-id]} :user-account} (fn-get-ctx)]
    (when user-id
      (fn-repo-get-acc-by-uid user-id))))

(defn save!
  [fn-repo-get-acc-by-uid fn-repo-save! {:keys [account/user-id] :as acc}]
  (let [existing-acc (fn-repo-get-acc-by-uid user-id)
        updated-acc  (if existing-acc
                       (medley/deep-merge existing-acc acc)
                       acc)]
    (fn-repo-save! updated-acc)))