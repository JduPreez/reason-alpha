(ns reason-alpha.data.repositories.position
  (:require [reason-alpha.model.portfolio-management :as portfolio-management]
            [reason-alpha.data.model :as data.model]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]))

(defn save!
  {:malli/schema
   [:=>
    [:cat
     :any
     portfolio-management/Position]
    portfolio-management/Position]}
  [db position]
  (data.model/save! db position))

(defn getn [db account-id]
  (let [positions (data.model/query
                   db
                   {:spec '{:find  [(pull h [*])]
                            :where [[h :position/account-id account-id]]
                            :in    [account-id]}})]
    positions))
