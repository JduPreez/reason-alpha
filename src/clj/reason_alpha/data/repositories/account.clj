(ns reason-alpha.data.repositories.account
  (:require [malli.core :as m]
            [reason-alpha.data.model :as data.model]
            [reason-alpha.model.accounts :as accounts]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.utils :as utils]))

(m/=> save! [:=>
             [:cat
              :any
              accounts/Account]
            accounts/Account])

(defn save! [db account]
  (let [acc (if (contains? account :account/creation-id)
              account
              (assoc account :account/creation-id (utils/new-uuid)))]
    (data.model/save! db acc {:role :system})))

(defn get-by-user-id [db user-id]
  (let [acc (-> (data.model/any
                 db
                 {:spec '{:find  [(pull e [*])]
                          :where [[e :account/user-id uid]]
                          :in    [uid]}
                  :args [user-id]
                  :role :system})
                first)]
    acc))
