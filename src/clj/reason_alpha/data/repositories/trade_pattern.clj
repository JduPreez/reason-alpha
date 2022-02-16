(ns reason-alpha.data.repositories.trade-pattern
  (:require [reason-alpha.data.model :as data.model]
            [reason-alpha.data-structures :as data-structs]
            [clojure.pprint :as pprint]))

(defn getn [db]
  (let [trade-patrns (data.model/query db
                                       {:spec '{:find  [(pull tp [*])]
                                                :where [[tp :trade-pattern/id]]}})]
    trade-patrns))

(defn get1 [db id]
  (data.model/any db [:trade-pattern/id := id]))

(defn save! [db {:keys [trade-pattern/parent-id
                        trade-pattern/id]
                 :as   trade-pattern}]
  (let [tp          (data.model/save! db trade-pattern)
        parent-tp   (when parent-id
                      (data.model/any db {:spec '{:find  [(pull tp [*])]
                                                  :where [[tp :trade-pattern/id id]]
                                                  :in    [id]}
                                          :args [parent-id]}))
        children-tp (when id
                      (data.model/query db {:spec '{:find  [(pull tp [*])]
                                                    :where [[tp :trade-pattern/parent-id id]]
                                                    :in    [id]}
                                            :args [id]}))
        tps         (data-structs/conj-ancestors-path (cond-> [tp]
                                                        parent-tp         (conj parent-tp)
                                                        (seq children-tp) (into children-tp))
                                                      :trade-pattern/parent-id
                                                      :trade-pattern/name
                                                      :trade-pattern/id
                                                      :trade-pattern/ancestors-path)]
    tps))

(comment
  (save! #:trade-pattern{:creation-id
                         #uuid "0ffbf0d7-c735-4a50-baa4-700ca41c2d72",
                         :parent-id
                         #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6",
                         :name
                         "ffff",
                         :description
                         "frrfrf"})

  )

(defn delete! [db trade-pattern-ids]
  (let [children   (data.model/query db {:spec '{:find  [(pull tp [*])]
                                                 :where [[tp :trade-pattern/parent-id id]]
                                                 :in    [[id ...]]}
                                         :args [trade-pattern-ids]})
        del-result (data.model/delete! db {:spec '{:find  [tp]
                                                   :where [(or [tp :trade-pattern/id id]
                                                               [tp :trade-pattern/parent-id id])]
                                                   :in    [[id ...]]}
                                           :args [trade-pattern-ids]})]
    #_(def del-reslt del-result)
    (-> children
        (as-> cdn (map #(select-keys % [:trade-pattern/id]) cdn))
        (into (map (fn [id] {:trade-pattern/id id}) trade-pattern-ids))
        (as-> di (assoc del-result :deleted-items di)))))

(comment
  (delete! '(#uuid "c7057fa6-f424-4b47-b1f2-de5ae63fb5fb"))

  del-reslt
  
  (getn)

  )
