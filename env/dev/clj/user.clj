(ns user
  (:require [reason-alpha.core :as reason-alpha]
            [reason-alpha.dev-data :as dev-data]
            [reason-alpha.infrastructure.server :as server]
            [reason-alpha.model :as model]))

(defn start
  ([load-test-data?]
   (when load-test-data?
     (dev-data/load-entity-test-data (::model/db model/system)))
   (reason-alpha/-main))
  ([]
   (reason-alpha/-main)))

(defn stop []
  (model/stop-system!))

(comment
  (dev-data/load-entity-test-data (::model/db model/system))

  (start true)

  (start)

  (stop)

)
