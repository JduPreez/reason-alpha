(ns reason-alpha.data.migrations
  (:require [clojure.java.jdbc :as jdbc]
            [migratus.core :as migratus]
            [reason-alpha.data :as data :refer [db connect]]))

(defn config [] 
  {:store                :database
   :migration-dir        "migrations/"
   :init-in-transaction? false
   :db                   (connect db)})

(defn run []
  (jdbc/db-do-commands data/db-config [(jdbc/create-table-ddl :schema_migrations
                                                              [[:id :bigint :primary :key]
                                                               [:applied :timestamp]
                                                               [:description :varchar]])])
  (migratus/migrate (config)))