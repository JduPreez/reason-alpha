(ns user
  (:require [mount.lite :as mount]
            [reason-alpha.core :as core]
            [reason-alpha.dev-data :as dev-data]))

(defn start
  ([migrate-db?]
   (dev-data/load-entity-test-data migrate-db?)
   (core/-main))
  ([]
   (core/-main)))

(defn stop []
  (mount/stop))
