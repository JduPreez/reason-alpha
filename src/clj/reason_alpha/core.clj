(ns reason-alpha.core
  (:require [malli.instrument :as malli.instr]
            [reason-alpha.data.model]
            [reason-alpha.model]
            [reason-alpha.model.portfolio-management]
            [reason-alpha.services.trade-pattern]))

(defn -main []
  #_(malli.instr/collect!)
  (malli.instr/instrument!))


