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

(defn get1 [db id]
  (let [m fin-instruments/InstrumentDto
        x (->> {:spec '{:find  [(pull e [*])]
                        :where [[e :instrument/id id]]
                        :in    [id]}
                :args [id]}
               (data.model/any db))
        y (mapping/command-ent->query-dto fin-instruments/InstrumentDto x)]
    (clojure.pprint/pprint {::get1 {:M m
                                    :X x
                                    :Y y}})
    y)
  #_(->> {:spec '{:find  [(pull e [*])]
                  :where [[e :instrument/id id]]
                  :in    [id]}
          :args [id]}
       (data.model/any db)
       (mapping/command-ent->query-dto fin-instruments/InstrumentDto)))

(defn delete! [db ids]
  (let [del-result (data.model/delete!
                    db
                    {:spec '{:find  [e acc-id]
                             :where [[e :instrument/id id]
                                     [e :instrument/account-id acc-id]]
                             :in    [[id ...]]}
                     :args [ids]})]
    (->> ids
         (mapv (fn [id] {:instrument/id id}))
         (assoc del-result :deleted-items))))
