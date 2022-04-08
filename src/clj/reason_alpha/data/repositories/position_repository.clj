(ns reason-alpha.data.repositories.position-repository
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
  (clojure.pprint/pprint {::save! position})
  (data.model/save! db position))

(defn getn [db account-id]
  (->> {:spec '{:find  [(pull pos [*])
                        (pull hold [*])
                        (pull tpattern [*])]
                :where [[pos :position/account-id account-id]
                        [(get-attr pos :position/holding-id nil) [hold ...]]
                        [(get-attr pos :position/trade-pattern-id nil) [tpattern ...]]]
                :in    [account-id]}
        :args [account-id]}
       (data.model/query db)
       (mapping/command-ents->query-dtos portfolio-management/PositionDto)))

(defn get1 [db id]
  (->> {:spec '{:find  [(pull pos [*])
                        (pull hold [*])
                        (pull tpattern [*])]
                :where [[pos :position/id id]
                        [(get-attr pos :position/holding-id nil) [hold ...]]
                        [(get-attr pos :position/trade-pattern-id nil) [tpattern ...]]]
                :in    [id]}
        :args [id]}
       (data.model/any db)
       (mapping/command-ent->query-dto portfolio-management/PositionDto)))

(defn delete! [db ids]
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
