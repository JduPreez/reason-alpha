(ns reason-alpha.data.repositories.position
  (:require [malli.core :as m]
            [reason-alpha.data.model :as data.model]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.model.portfolio-management :as portfolio-management]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]))

(m/=> save! [:=>
             [:cat
              :any
              portfolio-management/Position]
             portfolio-management/Position])

(defn save!
  [db position]
  (data.model/save! db position))

(defn getn [db account-id]
  (let [positions (data.model/query
                   db
                   {:spec '{:find  [(pull p [*])]
                            :where [[p :position/account-id account-id]]
                            :in    [account-id]}})]
    positions))
