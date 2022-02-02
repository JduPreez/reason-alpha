(ns reason-alpha.core
  (:require [malli.instrument :as malli.instr]
            [mount.lite :refer (defstate) :as mount]
            [reason-alpha.server :as server]
            [reason-alpha.data.model]
            [reason-alpha.model.portfolio-management]
            [reason-alpha.services.trade-pattern]))

(defn -main []
  (mount/start)
  (server/start!)
  (malli.instr/instrument!))


