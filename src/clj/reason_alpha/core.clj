(ns reason-alpha.core
  (:require [malli.instrument :as malli.instr]
            [reason-alpha.model :as model]))

(defn start []
  (model/start-system))

(defn stop []
  (model/stop-system!))

(defn -main []
  )


