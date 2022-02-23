(ns reason-alpha.data.repositories.account
  (:require [reason-alpha.data.model :as data.model]))

(defn get-by-user-id [db user-id]
  (let [acc (data.model/query
             db
             {:spec '{:find  [(pull a [*])]
                      :where [[a :account/user-id user-id]]
                      :in    [user-id]}})]
    acc))
