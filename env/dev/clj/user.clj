(ns user
  (:require [mount.lite :as mount]
            [reason-alpha.dev-data :as dev-data]))

(defn start
  ([load-test-data?]
   (when load-test-data?
     (dev-data/load-entity-test-data))
   (mount/start))
  ([]
   (mount/start)))

(defn stop []
  (mount/stop))

(comment
  (start true)

  (start)

  (stop)

)
