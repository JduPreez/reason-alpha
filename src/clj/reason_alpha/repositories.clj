(ns reason-alpha.repositories
  (:require [reason-alpha.infrastructure.db :as db]))

(defn add! [entity]
  0)

(defn remove! [spec]
  0)

(defn get>> [spec]
  (db/query spec))

(defn get> [spec]
  (db/query spec))