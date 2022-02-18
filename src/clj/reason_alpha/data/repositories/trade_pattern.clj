(ns reason-alpha.data.repositories.trade-pattern
  (:require [reason-alpha.data.model :as data.model]
            [reason-alpha.data-structures :as data-structs]
            [clojure.pprint :as pprint]))

(defn getn [db]
  (let [trade-patrns (data.model/query
                      db
                      {:spec '{:find  [(pull tp [*])]
                               :where [[tp :trade-pattern/id]]}})]
    trade-patrns))

(defn get1 [db id]
  (data.model/any db [:trade-pattern/id := id]))

(defn save! [db {:keys [trade-pattern/parent-id
                        trade-pattern/id]
                 :as   trade-pattern}]
  (let [tp          (data.model/save! db trade-pattern)
        #_#_parent-tp   (when parent-id
                      (data.model/any db {:spec '{:find  [(pull tp [*])]
                                                  :where [[tp :trade-pattern/id id]]
                                                  :in    [id]}
                                          :args [parent-id]}))
        #_#_children-tp (when id
                      (data.model/query db {:spec '{:find  [(pull tp [*])]
                                                    :where [[tp :trade-pattern/parent-id id]]
                                                    :in    [id]}
                                            :args [id]}))
        #_#_tps         (data-structs/conj-ancestors-path (cond-> [tp]
                                                        parent-tp         (conj parent-tp)
                                                        (seq children-tp) (into children-tp))
                                                      :trade-pattern/parent-id
                                                      :trade-pattern/name
                                                      :trade-pattern/id
                                                      :trade-pattern/ancestors-path)]
    #_tps
    tp))

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
    (-> children
        (as-> cdn (map #(select-keys % [:trade-pattern/id]) cdn))
        (into (map (fn [id] {:trade-pattern/id id}) trade-pattern-ids))
        (as-> di (assoc del-result :deleted-items di)))))
