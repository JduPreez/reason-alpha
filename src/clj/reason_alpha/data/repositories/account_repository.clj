(ns reason-alpha.data.repositories.account-repository
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
  (clojure.pprint/pprint {::$$$-SA account})
  (let [acc (if (contains? account :account/creation-id)
              account
              (assoc account :account/creation-id (utils/new-uuid)))]
    (data.model/save! db acc {:role :system})))

(defn get-by-user-id [db user-id]
  (let [acc (-> db
                (data.model/any
                 {:spec '{:find  [(pull e [*])]
                          :where [[e :account/user-id uid]]
                          :in    [uid]}
                  :args [user-id]
                  :role :system})
                first)]
    acc))

(defn get1 [db user-id]
  (->>  {:spec '{:find  [(pull e [*])]
                 :where [[e :account/user-id uid]]
                 :in    [uid]}
         :args [user-id]
         :role :system}
        (data.model/any db)
        (mapping/command-ent->query-dto accounts/AccountDto)))
