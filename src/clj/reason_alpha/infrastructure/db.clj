(ns reason-alpha.infrastructure.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.zip :as zip]))

(def db-schema "REASON-ALPHA")

(def db-config {:classname      "org.apache.ignite.IgniteJdbcThinDriver"
                :connection-uri "jdbc:ignite:thin://127.0.0.1"})

(defn connect
  ([]
   (jdbc/get-connection db-config))
  ([conf]
   (jdbc/get-connection conf)))

(defn- attribute-kv [table]
  (fn [[column val]]
    [(-> (str table "/" (name column))
         (string/replace #"_" "-")
         keyword) val]))

(defn- table-name [keywrd]
  (-> (namespace keywrd)
      (string/replace #"-" "_")))

(defn- full-table-name [keywrd]
  (-> (table-name keywrd)
      (#(str "\"" db-schema "\"." %))))

  #_(-> (str db-schema "." (namespace keywrd))
        (string/replace #"-" "_"))

(defn- column-name [keywrd]
  (-> (name keywrd)
      (string/replace #"-" "_")
      (#(str (full-table-name keywrd) "." %))))

(defn- combine-name [keywrd]
  (string/upper-case (name keywrd)))

(defn to-query
  "Only supports simple equality comparison conditions & non-nested combinators (AND/OR)"
  [spec]
  (reduce (fn [query condition]
            (let [is-vec         (vector? condition)
                  is-cond        (and is-vec
                                      (not (vector? (first condition))))
                  children       (when is-vec (count condition))
                  query-is-empty (empty? query)
                  default-sql-if #(if query-is-empty
                                    ; Add a column with the table name. This will be used when the
                                    ; result-set is mapped back to a Clojure map, so that we know
                                    ; what namespace to prefix keyword attributes with 
                                    (str "SELECT '" 
                                         (table-name %) 
                                         "' reason_alpha_table, * FROM " 
                                         (full-table-name %) 
                                         " WHERE")
                                    (first query))]
              (cond (and  is-cond (= children 3))
                    (let [[table-column comparison value] condition
                          ;tbl                             (full-table-name table-column)
                          col                             (column-name table-column)
                          comp                            (name comparison)
                          sql                             (str (default-sql-if table-column)
                                                               " " col " " comp " ?")]
                      (if query-is-empty
                        (conj [] sql value)
                        (-> (conj query value)
                            (assoc 0 'sql))))

                    (and is-cond (= children 4))
                    (let [[combine table-column comparison value] condition
                          ;tbl                                     (full-table-name table-column)
                          col                                     (column-name table-column)
                          comp                                    (name comparison)
                          sql                                     (str (default-sql-if table-column)
                                                                       " " (combine-name combine) " " col " " comp " ?")]
                      (if query-is-empty
                        (conj [] sql value)
                        (-> (conj query value)
                            (assoc 0 sql))))

                    :else query)))
          []
          (->> (zip/vector-zip spec)
               (iterate zip/next)
               (take-while #(not (zip/end? %)))
               (map zip/node))))

(defn- map-row [row]
  (let [table (:reason_alpha_table row)]
    (->> (dissoc row :reason_alpha_table)
         seq
         (mapcat (attribute-kv table))
         (apply hash-map))))

(defn query [query-spec]
  (->> (jdbc/query db-config (to-query query-spec))
       (map map-row)))

(comment
  (map-row {:id                 2
            :name               "Facebook"
            :owner_user_id      345
            :reason_alpha_table "security"})

  (jdbc/query db-config "SELECT 'security' table, security.*, 'user' table, user.user_name name FROM \"REASON-ALPHA\".security JOIN \"REASON-ALPHA\".user ON user.id = security.owner_user_id")
  
  (jdbc/insert-multi! db-config "\"REASON-ALPHA\".security" 
                      [{:id            1
                        :name          "Facebook"
                        :owner_user_id 1}
                       {:id            2
                        :name          "Facebook"
                        :owner_user_id 2}
                       {:id            3
                        :name          "Facebook"
                        :owner_user_id 3}])

  (query [[:security/name := "Facebook"]])

  (to-query [[:security/name := "Facebook"]
             [:or :security/owner-user-id := 4]
             [:and :security/name :<> "Playtech"]]))