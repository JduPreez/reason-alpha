(ns reason-alpha.services.account-service
  (:require [medley.core :as medley]
            [reason-alpha.model.utils :as mutils]
            [reason-alpha.utils :as utils]))

(defn get-account [fn-get-ctx fn-repo-get-acc-by-uid]
  (let [{{:keys [account/user-id]} :user-account} (fn-get-ctx)]
    (when user-id
      (fn-repo-get-acc-by-uid user-id))))

(defn get1 [fn-get-ctx fn-repo-get-acc-by-uid]
  (let [{{:keys [account/user-id]} :user-account
         send-msg                  :send-message} (fn-get-ctx)]
    (when-let [acc (and user-id
                        (fn-repo-get-acc-by-uid user-id))]
      (send-msg
       [:account.query/get1-result {:result acc
                                    :type   :success}]))))

(defn save-any!
  [fn-repo-get-acc-by-uid fn-repo-save! {:keys [account/user-id] :as acc}]
  (let [existing-acc (fn-repo-get-acc-by-uid user-id)
        updated-acc  (if existing-acc
                       (let [creation-id-k (mutils/creation-id-key :account acc)
                             existing-acc  (->> (utils/new-uuid)
                                                (get existing-acc creation-id-k)
                                                (assoc existing-acc creation-id-k))]
                         (medley/deep-merge existing-acc acc))
                       acc)
        updated-acc  (if-let [c (:account/currency updated-acc)]
                       updated-acc
                       (assoc updated-acc :account/currency :USD))
        saved-acc    (fn-repo-save! updated-acc)]
    saved-acc))

(defn save!
  [fn-get-acc fn-repo-save! {:keys [account/user-id] :as acc}]
  ;; A user can only update their own profile and subscriptions
  (let [a                           (select-keys
                                     acc
                                     [:accout/profile :account/subscriptions
                                      :account/currency])
        {existing-uid :account/user-id
         :as          existing-acc} (fn-get-acc)
        _                           (when (not= existing-uid user-id)
                                      (throw (ex-info "Unexpected user"
                                                      {:user-id      user-id
                                                       :existing-uid existing-uid})))
        updated-acc                 (if existing-acc
                                      (medley/deep-merge existing-acc a)
                                      a)]
    (fn-repo-save! updated-acc)))
