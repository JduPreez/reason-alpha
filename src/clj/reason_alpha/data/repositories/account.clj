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

(defn save! [db instr]
  (let [i (if (contains? instr :account/creation-id)
            instr
            (assoc instr :account/creation-id (utils/new-uuid)))]
    (data.model/save! db i)))

(defn get-by-user-id [db user-id]
  (let [acc (data.model/any
             db
             {:spec '{:find  [(pull e [*])]
                      :where [[e :account/user-id uid]]
                      :in    [uid]}
              :args [user-id]})]
    acc))
