(ns reason-alpha.data
  (:import [com.github.f4b6a3.uuid UuidCreator])
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.zip :as zip])
  (:gen-class))

(def db-schema "REASON-ALPHA")

(def db-config {:classname      "org.apache.ignite.IgniteJdbcThinDriver"
                :connection-uri "jdbc:ignite:thin://127.0.0.1"
                :db-schema      "REASON-ALPHA"})

(defn sql-name [str] 
  (string/replace str #"-" "_"))

(defn- attribute-kv [table [column val]]
  [(-> (str table "/" (name column))
       (string/replace #"_" "-")
       keyword) val])

(defn- add-schema [table]
  (str "\"" db-schema "\"." table))

(defn- table-name [keywrd]
  (sql-name (namespace keywrd)))

(defn- full-table-name [keywrd]
  (-> (table-name keywrd)
      add-schema))

(defn- column-name [keywrd]
  (-> (name keywrd)
      sql-name
      (#(str (full-table-name keywrd) "." %))))

(defn- column-name-kw [attr]
  (let [name (sql-name (name attr))
        ns   (sql-name (namespace attr))]
    (keyword (str ns "/" name))))

(defn- combine-name [keywrd]
  (string/upper-case (name keywrd)))

(defn- maybe-default-sql [query table-column with-where]
  (if (empty? query)
    ;; Add a column with the table name. This will be used when the
    ;; result-set is mapped back to a Clojure map, so that we know
    ;; what namespace to prefix keyword attributes with 
    (str "SELECT '"
         (table-name table-column)
         "' reason_alpha_table, * FROM "
         (full-table-name table-column)
         (when (true? with-where) " WHERE"))
    (first query)))

(defn- to-query
  "Only supports simple equality comparison conditions & non-nested combinators (AND/OR)"
  [spec]
  (reduce (fn [query condition]
            (let [is-vec         (vector? condition)
                  is-cond        (and is-vec
                                      (not (vector? (first condition))))
                  children       (when is-vec (count condition))
                  query-is-empty (empty? query)]
              (cond (and  is-cond (= children 3))
                    (let [[table-column comparison value] condition
                          col                             (column-name table-column)
                          comp                            (name comparison)
                          sql                             (str (maybe-default-sql query table-column true)
                                                               " " col " " comp " ?")]
                      (if query-is-empty
                        (conj [] sql value)
                        (-> (conj query value)
                            (assoc 0 'sql))))

                    (and is-cond (= children 4))
                    (let [[combine table-column comparison value] condition
                          col                                     (column-name table-column)
                          comp                                    (name comparison)
                          sql                                     (str (maybe-default-sql query table-column true)
                                                                       " " (combine-name combine) " " col " " comp " ?")]
                      (if query-is-empty
                        (conj [] sql value)
                        (-> (conj query value)
                            (assoc 0 sql))))

                    (and is-cond (= children 1))
                    (let [[table-column] condition]
                      (maybe-default-sql query table-column false))

                    :else query)))
          []
          (->> (zip/vector-zip spec)
               (iterate zip/next)
               (take-while #(not (zip/end? %)))
               (map zip/node))))

(comment

  (to-query [[:security/name := "Facebook"]
             [:or :security/owner-user-id := 4]
             #_[:and :security/name :<> "Playtech"]])

  (to-query [[:security/*]])

)

(defn- row->entity [{table :reason_alpha_table
                     :as   row}]
  (->> (dissoc row :reason_alpha_table)
       seq
       (mapcat (partial attribute-kv table))
       (apply hash-map)))

(defn- ent-name->id [ent-name]
  (keyword (str ent-name "/id")))

(defn- ent-name->db-id [ent-name]
  (column-name-kw (ent-name->id ent-name)))

(defn- entities->add-all-cmd [entities]
  (let [attr->column-fn #(-> %
                             name
                             sql-name
                             keyword)
        id              (-> (peek entities)
                            keys
                            first
                            namespace
                            ent-name->db-id)
        table           (full-table-name id)
        rows            (->> entities
                             (map (fn [entity]
                                    (let [attrs (seq entity)
                                          row   (->> attrs
                                                     (map (fn [[attr val]]
                                                            [(attr->column-fn attr) val]))
                                                     flatten
                                                     (apply hash-map))]
                                      (assoc row (attr->column-fn id) (UuidCreator/getLexicalOrderGuid)))))
                             vec)]

    {:table table
     :rows  rows}))

(defn- rentity->save-cmds [rentity]
  (->> (seq rentity)
       (group-by (fn [[attr _]] (namespace attr)))
       seq
       (map (fn [[entity [[attr] :as attrs]]]
              (let [db-id  (ent-name->db-id entity)
                    id     (ent-name->id entity)
                    table  (full-table-name attr)
                    entity (->> attrs
                                flatten
                                (apply hash-map))
                    row    (->> attrs
                                (map (fn [[attr val]]
                                       [(column-name-kw attr) val]))
                                flatten
                                (apply hash-map))]
                (if (contains? row db-id)
                  {:type   :data/update
                   :table  table
                   :row    row
                   :entity entity
                   :id     db-id}
                  (let [id-val (UuidCreator/getLexicalOrderGuid)]
                    {:type   :data/insert
                     :table  table
                     :row    (assoc row db-id id-val)
                     :entity (assoc entity id id-val)
                     :id     db-id})))))))

(defn- save-impl!
  "Updates columns or inserts a new row in the specified table"
  [{type   :type
    table  :table
    row    :row
    entity :entity
    id     :id}]
  (if (= :data/update type)
    (jdbc/update! db-config table (dissoc row id)
                  ["id = ?" (id row)])
    (jdbc/insert! db-config table row))
  entity)

(defn- add-all-impl! [{table :table
                       rows  :rows}]
  (jdbc/insert-multi! db-config table rows)
  rows)

(defprotocol DataBase
  (connect [_])
  (choose [_ query-spec])
  (any [this query-spec])
  (save! ; todo: Do this in a transaction
    [this rentity]
    [_ rentity save-impl!-fn]
    "rentity = Root-Entity:
   Each keyword ns constitutes an entity, and together they
   are the root entity.")
  (add-all!
    [this entities]
    [_ entities add-all-impl!-fn]
    "Inserts multiple entities of the same type.")
  #_(remove! [_ rentity]))

(def db (let [row->ent     row->entity
              db-conf      db-config
              to-qry       to-query
              sav-impl!    save-impl!
              add-al-impl! add-all-impl!]
          (reify DataBase
            (connect [_] (jdbc/get-connection db-conf))

            (choose [_ query-spec]
              (->> (jdbc/query db-conf (to-qry query-spec))
                   (map row->ent)))

            (any [this query-spec]
              (first (choose this query-spec)))

            (save! [this rentity]
              (save! this rentity sav-impl!))
;; TODO: Save should return entity clj, not row sql format
            (save! [_ rentity save-impl!-fn]
              (->> rentity
                   rentity->save-cmds
                   (map save-impl!-fn)))

            (add-all! [this entities]
              (add-all! this entities add-al-impl!))

            (add-all! [_ entities add-all-impl!-fn]
              (->> entities
                   entities->add-all-cmd
                   add-all-impl!-fn))

            #_(remove! [_ rentity]))))

(comment

  (rentity->save-cmds {:trade-pattern/name "Buy Support or Short Resistance",
                       :trade-pattern/creation-id nil,
                       :trade-pattern/id #uuid "01738610-a026-1f53-5d94-219803fa47e1",
                       :trade-pattern/parent-id
                       #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6",
                       :trade-pattern/user-id #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f",
                       :trade-pattern/description "another test"})
  )
