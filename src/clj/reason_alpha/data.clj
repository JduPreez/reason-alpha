(ns reason-alpha.data
  (:require [clojure.java.jdbc :as jdbc]))

(def db-config {:classname      "org.apache.ignite.IgniteJdbcThinDriver"
                :connection-uri "jdbc:ignite:thin://127.0.0.1"})

(defn db
  ([]
   (jdbc/get-connection db-conf))
  ([db-conf]
   (jdbc/get-connection db-conf)))

