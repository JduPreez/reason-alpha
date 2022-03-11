(ns reason-alpha.data.repositories.account
  (:require [malli.core :as m]
            [reason-alpha.data.model :as data.model]
            [reason-alpha.model.accounts :as accounts]
            [reason-alpha.model.mapping :as mapping]
            ))

(m/=> save! [:=>
             [:cat
              :any
              accounts/Account]
            accounts/Account])

(defn save!
  [db instr]
  (data.model/save! db instr))

(defn get-by-user-id [db user-id]
  (let [acc (data.model/query
             db
             {:spec '{:find  [(pull a [*])]
                      :where [[a :account/user-id user-id]]
                      :in    [user-id]}})]
    acc))
