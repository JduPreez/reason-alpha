(ns reason-alpha.services.trades
  (:require [reason-alpha.data :refer [query save! delete! any]]
            [reason-alpha.data.crux :as data.crux]
            [reason-alpha.data-structures :as data-structs]))

(defn get-trade-patterns []
  (let [trade-patrns (query data.crux/db
                            {:spec '{:find  [(pull tp [*])]
                                     :where [[tp :trade-pattern/id]]}})]
    trade-patrns))

(defn save-trade-pattern! [{:keys [trade-pattern/parent-id
                                   trade-pattern/id]
                            :as   trade-pattern}]
  (let [tp          (save! data.crux/db trade-pattern)
        parent-tp   (when parent-id
                      (any data.crux/db {:spec '{:find  [(pull tp [*])]
                                                 :where [[tp :trade-pattern/id id]]
                                                 :in    [id]}
                                         :args [parent-id]}))
        children-tp (query data.crux/db {:spec '{:find  [(pull tp [*])]
                                                 :where [[tp :trade-pattern/parent-id id]]
                                                 :in    [id]}
                                         :args [id]})
        tps         (data-structs/conj-ancestors-path (cond-> [tp]
                                                        parent-tp         (conj parent-tp)
                                                        (seq children-tp) (into children-tp))
                                                      :trade-pattern/parent-id
                                                      :trade-pattern/name
                                                      :trade-pattern/id
                                                      :trade-pattern/ancestors-path)]
    tps))

(defn delete-trade-patterns! [trade-pattern-ids]
  (let [children   (query data.crux/db {:spec '{:find  [(pull tp [*])]
                                                :where [[tp :trade-pattern/parent-id id]]
                                                :in    [[id ...]]}
                                        :args [trade-pattern-ids]})
        del-result (delete! data.crux/db {:spec '{:find  [tp]
                                                  :where [(or [tp :trade-pattern/id id]
                                                              [tp :trade-pattern/parent-id id])]
                                                  :in    [[id ...]]}
                                          :args [trade-pattern-ids]})]
    (-> children
        (as-> cdn (map #(select-keys % [:trade-pattern/id]) cdn))
        (into (map (fn [id] {:trade-pattern/id id}) trade-pattern-ids))
        (as-> di (assoc del-result :deleted-items di)))))
