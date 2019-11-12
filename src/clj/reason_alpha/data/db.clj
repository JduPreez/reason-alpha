(ns reason-alpha.data.db
  (:import [com.github.f4b6a3.uuid UuidCreator])
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

(defn- add-schema [table]
  (str "\"" db-schema "\"." table))

(defn- table-name-str [table]
  (string/replace table #"-" "_"))

(defn- table-name [keywrd]
  (-> (namespace keywrd)
      table-name-str))

(defn- full-table-name-str [table]
  (-> (table-name-str table)
      add-schema))

(defn- full-table-name [keywrd]
  (-> (table-name keywrd)
      add-schema))

(defn- column-name [keywrd]
  (-> (name keywrd)
      (string/replace #"-" "_")
      (#(str (full-table-name keywrd) "." %))))

(defn- column-name-kw [attr]
  (let [name (string/replace (name attr) #"-" "_")
        ns   (string/replace (namespace attr) #"-" "_")]
    (keyword (str ns "/" name))))

(defn- combine-name [keywrd]
  (string/upper-case (name keywrd)))

(defn- to-query
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

(defn- to-entity [row]
  (let [table (:reason_alpha_table row)]
    (->> (dissoc row :reason_alpha_table)
         seq
         (mapcat (attribute-kv table))
         (apply hash-map))))

(defn get>> [query-spec]
  (->> (jdbc/query db-config (to-query query-spec))
       (map to-entity)))

(defn get> [query-spec]
  (first (get>> query-spec)))

(defn- update-or-insert!
  "Updates columns or inserts a new row in the specified table"
  [type table row & [id]]
  (if (= ::update type)
    (jdbc/update! db-config table row 
                  [(str (column-name-kw id) " = ?" (id row))])
    (jdbc/insert! db-config table row)))

(defn- save-entity! [save!-fn]
  (fn [[entity [[attr] :as attrs]]]
    (let [id          (column-name-kw
                       (keyword (str entity "/id")))
          full-tbl-nm (full-table-name attr)
          row         (->> attrs
                           (map (fn [[attr val]]
                                  [(column-name-kw attr) val]))
                           flatten
                           (apply hash-map))]
      (if (contains? row id)
        (save!-fn ::update full-tbl-nm row id)
        (save!-fn ::insert full-tbl-nm (assoc row id (UuidCreator/getLexicalOrderGuid)) id)))))

; TODO: Do this in a transaction
; TODO: Test update scenario
(defn save!
  "rentity = Root-Entity"
  ([rentity]
   (save! rentity update-or-insert!))
  ([rentity save!-fn]
   (->> (seq rentity)
        (group-by (fn [[attr _]] (namespace attr)))
        seq
        (map (save-entity! save!-fn)))))

(defn delete! [rentity] nil)

(comment
  (save! {:security/name          "Facebook"
          :security/owner-user-id 5
          :user/user-name         "Frikkie"
          :user/email             "j@j.com"})

  (contains? {:security/id            0
              :security/name          "Facebook"
              :security/owner-user-id 5
              :user/user-name         "Frikkie"
              :user/email             "j@j.com"} :id)

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