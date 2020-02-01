(ns user
  (:require [reason-alpha.core :as core]
            [reason-alpha.dev-data :as dev-data]))

(defn start []
  (dev-data/load-entity-test-data true)
  (core/-main))