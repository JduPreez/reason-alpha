(ns user
  (:require [mount.lite :as mount]
            [reason-alpha.dev-data :as dev-data]
            [reason-alpha.core :as reason-alpha]))

(defn start
  ([load-test-data?]
   (when load-test-data?
     (dev-data/load-entity-test-data))
   (reason-alpha/-main)
   (mount/start))
  ([]
   (mount/start)))

(defn stop []
  (mount/stop))

(comment
  (dev-data/load-entity-test-data)

  (start true)

  (start)

  (stop)

)
