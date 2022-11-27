(ns reason-alpha.data.repositories.trade-pattern-repository
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

(defn get1 [db id]
  (->> {:spec '{:find  [(pull e [*])]
                :where [[e :trade-pattern/id id]]
                :in    [id]}
        :args [id]}
       (data.model/any db)
       (mapping/command-ent->query-dto portfolio-management/TradePatternDto)))

(defn save! [db tpattern]
  (data.model/save! db tpattern))

(defn delete! [db ids]
  (let [children   (data.model/query db {:spec '{:find  [(pull e [*])]
                                                 :where [[e :trade-pattern/parent-id id]]
                                                 :in    [[id ...]]}
                                         :args [ids]})
        del-result (data.model/delete! db {:spec '{:find  [e acc-id]
                                                   :where [(or (and [e :trade-pattern/id id]
                                                                    [e :trade-pattern/account-id acc-id])
                                                               (and [e :trade-pattern/parent-id id]
                                                                    [e :trade-pattern/account-id acc-id]))]
                                                   :in    [[id ...]]}
                                           :args [ids]})]
    (-> children
        (as-> cdn (map #(select-keys % [:trade-pattern/id]) cdn))
        (into (map (fn [id] {:trade-pattern/id id}) ids))
        (as-> di (assoc del-result :deleted-items di)))))

(defn get-trade-patterns-with-positions [db trade-pattern-ids]
  (->> {:spec '{:find  [(pull tp [*])]
                :where [(or [tp :trade-pattern/id tpid]
                            [tp :trade-pattern/parent-id tpid])
                        [p :position/trade-pattern-id tpid]]
                :in    [[tpid ...]]}
        :args [trade-pattern-ids]}
       (data.model/query db)
       (mapping/command-ents->query-dtos portfolio-management/TradePatternDto)))
