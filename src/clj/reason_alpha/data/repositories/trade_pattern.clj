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

(defn save! [db tpattern]
  (data.model/save! db tpattern))

(defn delete! [db account-id trade-pattern-ids]
  (let [children   (data.model/query db {:spec '{:find  [(pull e [*])]
                                                 :where [[e :trade-pattern/parent-id id]
                                                         [e :trade-pattern/account-id acc-id]]
                                                 :in    [acc-id [id ...]]}
                                         :args [account-id trade-pattern-ids]})
        del-result (data.model/delete! db {:spec '{:find  [e]
                                                   :where [(or (and [e :trade-pattern/id id]
                                                                    [e :trade-pattern/account-id acc-id])
                                                               (and [e :trade-pattern/parent-id id]
                                                                    [e :trade-pattern/account-id acc-id]))]
                                                   :in    [acc-id [id ...]]}
                                           :args [account-id trade-pattern-ids]})]
    (-> children
        (as-> cdn (map #(select-keys % [:trade-pattern/id]) cdn))
        (into (map (fn [id] {:trade-pattern/id id}) trade-pattern-ids))
        (as-> di (assoc del-result :deleted-items di)))))
