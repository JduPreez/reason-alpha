(ns reason-alpha.data.repositories.instrument
  (:require [malli.core :as m]
            [reason-alpha.data.model :as data.model]
            [reason-alpha.model.fin-instruments :as fin-instruments]
            [reason-alpha.model.mapping :as mapping]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]))

(m/=> save! [:=>
             [:cat
              :any
              fin-instruments/Instrument]
             fin-instruments/Instrument])

(defn save!
  [db instr]
  (data.model/save! db instr))

(defn getn [db account-id]
  (->> {:spec '{:find  [(pull e [*])]
                :where [[e :instrument/account-id account-id]]
                :in    [account-id]}
        :args [account-id]}
       (data.model/query db)
       (mapping/command-ents->query-dtos fin-instruments/InstrumentDto)))

(defn get1 [db account-id id]
  (->> {:spec '{:find  [(pull e [*])]
                :where [[e :instrument/id id]
                        [e :instrument/account-id account-id]]
                :in    [[account-id id]]}
        :args [account-id id]}
       (data.model/any db)
       (mapping/command-ent->query-dto fin-instruments/InstrumentDto)))

(defn delete! [db account-id ids]
  (let [del-result (data.model/delete!
                    db
                    {:spec '{:find  [e]
                             :where [[e :instrument/id id]
                                     [e :instrument/account-id acc-id]]
                             :in    [acc-id [id ...]]}
                     :args [account-id ids]})]
    (->> ids
         (mapv (fn [id] {:instrument/id id}))
         (assoc del-result :deleted-items))))
