(ns reason-alpha.data.repositories.holding-repository
  (:require [malli.core :as m]
            [reason-alpha.data.model :as data.model]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.model.portfolio-management :as portfolio-management]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]))

(m/=> save-holding! [:=>
                     [:cat
                      :any
                      portfolio-management/Holding]
                     portfolio-management/Holding])

(defn save-holding!
  [db holding]
  (data.model/save! db holding))

(defn get-holdings [db {:keys [account-id role]}]
  (->> {:spec '{:find  [(pull e [*])]
                :where [[e :holding/account-id account-id]]
                :in    [account-id]}
        :role (or role :member)
        :args [account-id]}
       (data.model/query db)
       (mapping/command-ents->query-dtos portfolio-management/HoldingDto)))

(defn get-holding [db id]
  (->> {:spec '{:find  [(pull e [*])]
                :where [[e :holding/id id]]
                :in    [id]}
        :args [id]}
       (data.model/any db)
       (mapping/command-ent->query-dto portfolio-management/HoldingDto)))

(defn delete-holdings! [db ids]
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

(defn get-holdings-positions [db {:keys [account-id role]}]
  (->> {:spec '{:find  [(pull pos [*])
                        (pull hold [*])
                        (pull tpattern [*])]
                :where [[pos :position/account-id account-id]
                        [(get-attr pos :position/holding-id nil) [hold ...]]
                        [(get-attr pos :position/trade-pattern-id nil) [tpattern ...]]]
                :in    [account-id]}
        :args [account-id]
        :role (or role :member)}
       (data.model/query db)
       (mapping/command-ents->query-dtos portfolio-management/PositionDto)))

(defn get-holding-positions [db id]
  (->> {:spec '{:find  [(pull pos [*])
                        (pull hold [*])
                        (pull tpattern [*])]
                :where [(or [pos :position/id id]
                            [pos :position/holding-position-id id])
                        [(get-attr pos :position/holding-id nil) [hold ...]]
                        [(get-attr pos :position/trade-pattern-id nil) [tpattern ...]]]
                :in    [id]}
        :args [id]}
       (data.model/query db)
       (mapping/command-ents->query-dtos portfolio-management/PositionDto)))

(defn delete-positions! [db ids]
  (let [del-result (data.model/delete!
                    db
                    {:spec '{:find  [e acc-id]
                             :where [[e :position/id id]
                                     [e :position/account-id acc-id]]
                             :in    [[id ...]]}
                     :args [ids]})]
    (->> ids
         (mapv (fn [id] {:position/id id}))
         (assoc del-result :deleted-items))))

(m/=> save-position! [:=>
                      [:cat
                       :any
                       portfolio-management/Position]
                      portfolio-management/Position])

(defn save-position!
  [db position]
  (data.model/save! db position))
