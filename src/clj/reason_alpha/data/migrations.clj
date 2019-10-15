(ns reason-alpha.data.migrations
  (:require [migratus.core :as migratus]))

(def config {:store                :database
             :migration-dir        "migrations/"
             :init-in-transaction? false
             :db                   connection})

(comment  
  (jdbc/db-do-commands db-conf [(jdbc/create-table-ddl :schema_migrations
                                                       [[:id :bigint :primary :key]
                                                        [:applied :timestamp]
                                                        [:description :varchar]])])
  (migratus/migrate config))