(ns reason-alpha.data.repositories.instrument
  (:require [malli.core :as m]
            [reason-alpha.data.model :as data.model]
            [reason-alpha.model.fin-instruments :as fin-instruments]
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
  (let [instruments (data.model/query
                     db
                     {:spec '{:find  [(pull p [*])]
                              :where [[p :instrument/account-id account-id]]
                              :in    [account-id]}})]
    instruments))

(defn get1 [db account-id id]
  (data.model/any db {:spec '{:find  [(pull i [*])]
                              :where [[i :instrument/id id]
                                      [i :instrument/account-id account-id]]
                              :in    [[account-id id]]}
                      :args [account-id id]}))
