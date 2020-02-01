(ns reason-alpha.data.management
  (:require [clojure.string :as string]
            [clojure.java.jdbc :as jdbc]
            [migratus.core :as migratus]
            [reason-alpha.data :as data :refer [db connect]]))

(defn config [] 
  {:store                :database
   :migration-dir        "migrations/"
   :init-in-transaction? false
   :db                   (connect db)})

(defn keyword->sql-obj [kw]
  (string/upper-case (name kw)))

(defn metadata [sql-obj-types]
  (let [sots (map #(string/upper-case (name %)) sql-obj-types)]
    (with-open [conn (connect db)]
      (into []
            (jdbc/result-set-seq
             (-> conn
                 (.getMetaData)
                 (.getTables nil nil nil
                             (into-array sots))))))))

(defn exists? [sql-obj-type sql-obj & [schema]]
  (not 
   (nil? (some #(and (= (:table_name %) sql-obj)
                     (= (:table_schem %) (if schema
                                           schema
                                           "PUBLIC"))) (metadata [sql-obj-type])))))

(defn migrate []
  (when (not (exists? :table "SCHEMA_MIGRATIONS"))
    (jdbc/db-do-commands data/db-config [(jdbc/create-table-ddl :schema_migrations
                                                                [[:id :bigint :primary :key]
                                                                 [:applied :timestamp]
                                                                 [:description :varchar]])]))
  (migratus/migrate (config)))

(comment
  (migrate)
  
  (metadata [:table :view])
  
  (exists? :table "SCHEMA_MIGRATIONS")
  
  (migratus/migrate (config)))