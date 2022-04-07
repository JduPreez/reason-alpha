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
  (->> {:spec '{:find  [(pull pos [*]) (pull instr [*])]
                :where [[pos :position/account-id account-id]
                        [pos :position/instrument-id instr]
                        [instr :holding/instrument-name instr-nm]]
                :in    [account-id]}
        :args [account-id]}
       (data.model/query db)
       (mapping/command-ents->query-dtos portfolio-management/PositionDto)))

(defn get1 [db id]
  (->> {:spec '{:find  [(pull pos [*]) (pull instr [*])]
                :where [[pos :position/id id]
                        [pos :position/instrument-id instr]
                        [instr :holding/instrument-name instr-nm]]
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
