(ns reason-alpha.services.account)

(defn get-account [fn-get-ctx fn-get-acc-by-user-id data]
  (let [{{:keys [account/user-id]} :user-account} (fn-get-ctx data)]
    (when user-id
      (fn-get-acc-by-user-id user-id))))
