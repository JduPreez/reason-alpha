(ns reason-alpha.infrastructure.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.zip :as zip]))

(def db-schema "REASON-ALPHA")

(defn table-name [keywrd]
  (-> (str db-schema "." (namespace keywrd))
      (string/replace #"-" "_")))

(defn column-name [keywrd]
  (-> (str (table-name keywrd) "." (name keywrd))
      (string/replace #"-" "_")))

(defn combine-name [keywrd]
  (string/capitalize (name keywrd)))

(defn to-sql 
  "Only supports simple equality comparison conditions & non-nested combinators (AND/OR)"
  [spec]
  (reduce (fn [query condition]
            (let [is-vec         (vector? condition)
                  is-cond        (and is-vec
                                      (not (vector? (first condition))))
                  children       (when is-vec (count condition))
                  query-is-empty (empty? query)
                  default-sql-if #(if query-is-empty
                                    (str "SELECT * FROM " % " WHERE")
                                    (first query))]
              (cond (and  is-cond (= children 3))
                    (let [[table-column comparison value] condition
                          tbl                             (table-name table-column)
                          col                             (column-name table-column)
                          comp                            (name comparison)
                          sql                             (str (default-sql-if tbl)
                                                               " " col " " comp " ?")]
                      (println sql value) ; TODO: Change this to add SQL text to front & value to back
                      (if query-is-empty
                        (conj () sql value)
                        (conj (pop query) 'sql)))

                    (and is-cond (= children 4))
                    (let [[combine table-column comparison value] condition
                          tbl                                 (table-name table-column)
                          col                                 (column-name table-column)
                          comp                                (name comparison)
                          sql'                                (str (default-sql-if tbl)
                                                                   " " (combine-name combine) " " col " " comp " ?")]
                      (if query-is-empty
                        '(sql' value)
                        (conj (pop query) sql' value)))

                    :else query)))
          '()
          (->> (zip/vector-zip spec)
               (iterate zip/next)
               (take-while #(not (zip/end? %)))
               (map zip/node))))

(comment
 (to-sql [[:security/name := "Facebook"]])

  (to-sql [[:security/name := "Facebook"]
           [:or :security/owner-user-id := 4]
           [:and :security/name :<> "Playtech"]])

  #_(defn to-sql [spec])
  (loop [loc spec]
    (if (zip/end? loc)
      (zip/root loc)
      (recur (zip/next))))))