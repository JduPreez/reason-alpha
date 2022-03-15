(ns reason-alpha.data.repositories.trade-pattern
  (:require [reason-alpha.data.model :as data.model]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.model.portfolio-management :as portfolio-management]))

(defn getn [db account-id]
  (->> {:spec '{:find  [(pull e [*])]
                :where [[e :trade-pattern/account-id account-id]]
                :in    [account-id]}
        :args [account-id]}
       (data.model/query db)
       (mapping/command-ents->query-dtos portfolio-management/TradePatternDto)))

(defn get1 [db account-id id]
  (->> {:spec '{:find  [(pull e [*])]
                :where [[e :trade-pattern/id id]
                        [e :trade-pattern/account-id account-id]]
                :in    [[account-id id]]}
        :args [account-id id]}
       (data.model/any db)
       (mapping/command-ent->query-dto portfolio-management/TradePatternDto)))

(defn save! [db {:keys [trade-pattern/parent-id
                        trade-pattern/id]
                 :as   trade-pattern}]
  (let [tp (data.model/save! db trade-pattern)]
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
