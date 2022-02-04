(ns reason-alpha.core
  (:require [malli.instrument :as malli.instr]
            [mount.lite :as mount]
            [reason-alpha.data.model]
            [reason-alpha.model.portfolio-management]
            [reason-alpha.services.trade-pattern]))

(defn -main []
  (mount/start)
  #_(server/start!)
  #_(malli.instr/collect!)
  (malli.instr/instrument!)
  )


