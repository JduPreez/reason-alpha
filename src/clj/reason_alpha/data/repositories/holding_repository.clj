(ns reason-alpha.data.repositories.holding-repository
  (:require [malli.core :as m]
            [reason-alpha.data.model :as data.model]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.model.portfolio-management :as portfolio-management]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]))

(m/=> save! [:=>
             [:cat
              :any
              portfolio-management/Holding]
             portfolio-management/Holding])

(defn save!
  [db instr]
  (data.model/save! db instr))

(defn getn [db account-id]
  (->> {:spec '{:find  [(pull e [*])]
                :where [[e :holding/account-id account-id]]
                :in    [account-id]}
        :args [account-id]}
       (data.model/query db)
       (mapping/command-ents->query-dtos portfolio-management/HoldingDto)))

(defn get1 [db id]
  (->> {:spec '{:find  [(pull e [*])]
                :where [[e :holding/id id]]
                :in    [id]}
        :args [id]}
       (data.model/any db)
       (mapping/command-ent->query-dto portfolio-management/HoldingDto)))

(defn delete! [db ids]
  (let [del-result (data.model/delete!
                    db
                    {:spec '{:find  [e acc-id]
                             :where [[e :holding/id id]
                                     [e :holding/account-id acc-id]]
                             :in    [[id ...]]}
                     :args [ids]})]
    (->> ids
         (mapv (fn [id] {:holding/id id}))
         (assoc del-result :deleted-items))))
