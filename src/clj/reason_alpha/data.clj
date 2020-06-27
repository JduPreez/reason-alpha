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
    ; Add a column with the table name. This will be used when the
    ; result-set is mapped back to a Clojure map, so that we know
    ; what namespace to prefix keyword attributes with 
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
            #_(clojure.pprint/pprint {:q query
                                      :c condition})
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

(defn- row->entity [row]
  (let [table (:reason_alpha_table row)]
    (->> (dissoc row :reason_alpha_table)
         seq
         (mapcat (partial attribute-kv table))
         (apply hash-map))))

(defn- ent-name->id [ent-name]
  (column-name-kw
   (keyword (str ent-name "/id"))))

(defn- entities->add-all-cmd [entities]
  (let [attr->column-fn #(-> %
                             name
                             sql-name
                             keyword)
        id              (-> (peek entities)
                            keys
                            first
                            namespace
                            ent-name->id)
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
              (let [id    (ent-name->id entity)
                    table (full-table-name attr)
                    row   (->> attrs
                               (map (fn [[attr val]]
                                      [(column-name-kw attr) val]))
                               flatten
                               (apply hash-map))]
                (if (contains? row id)
                  {:type  :data/update
                   :table table
                   :row   row
                   :id    id}
                  {:type  :data/insert
                   :table table
                   :row   (assoc row id (UuidCreator/getLexicalOrderGuid))
                   :id    id}))))))

(defn- save-impl!
  "Updates columns or inserts a new row in the specified table"
  [{type  :type
    table :table
    row   :row
    id    :id}]
  (if (= :data/update type)
    (jdbc/update! db-config table row
                  [(str (column-name-kw id) " = ?" (id row))])
    (jdbc/insert! db-config table row))
  row)

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

  (let [sid (UuidCreator/getLexicalOrderGuid)
        uid (UuidCreator/getLexicalOrderGuid)]
    (save! {:security/id            sid
            :security/name          "Facebook"
            :security/owner-user-id 5
            :user/id                uid
            :user/user-name         "Frikkie"
            :user/email             "j@j.com"} (fn [type _ rw _] {:type type
                                                                  :data rw
                                                                  :sid  (= sid (:security/id rw))
                                                                  :uid  uid})))


  (let [rentity {:security/id            0
                 :security/name          "Facebook"
                 :security/owner-user-id 5
                 :user/user-name         "Frikkie"
                 :user/email             "j@j.com"}]
    (->> (seq rentity)
         (group-by (fn [[attr _]] (namespace attr)))))

  (->> [{:security/name          "Facebook"
         :security/owner-user-id 1}
        {:security/name          "Exxon Mobil"
         :security/owner-user-id 2}
        {:security/name          "Microsoft"
         :security/owner-user-id 3}
        {:security/name          "GSK"
         :security/owner-user-id 4}]
       entities->add-all-cmd)

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

  (doall (choose db [[:trade-pattern/user-id := "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"]]))

  (doall (choose db [[:trade-pattern/*]]))

  (let [row   {:id                 2
               :name               "Facebook"
               :owner_user_id      345
               :reason_alpha_table "security"}
        table (:reason_alpha_table row)]
    (->> (dissoc row :reason_alpha_table)
         seq
         (mapcat (partial attribute-kv table))
         (apply hash-map)))
  (attribute-kv "security" [:owner_user_id 2])
 
  )
