(ns reason-alpha.data.repositories.trade-pattern
  (:require [reason-alpha.data :as data]
            [reason-alpha.data.crux :as data.crux]
            [reason-alpha.data-structures :as data-structs]
            [clojure.pprint :as pprint]))

(defn getn []
  (let [trade-patrns (data/query data.crux/db
                                 {:spec '{:find  [(pull tp [*])]
                                          :where [[tp :trade-pattern/id]]}})]
    trade-patrns))

(defn get1 [id]
  (data/any data.crux/db [:trade-pattern/id := id]))

(defn save! [{:keys [trade-pattern/parent-id
                     trade-pattern/id]
              :as   trade-pattern}]
  (pprint/pprint {::save! trade-pattern})
  (let [tp          (data/save! data.crux/db trade-pattern)
        parent-tp   (when parent-id
                      (data/any data.crux/db {:spec '{:find  [(pull tp [*])]
                                                      :where [[tp :trade-pattern/id id]]
                                                      :in    [id]}
                                              :args [parent-id]}))
        children-tp (data/query data.crux/db {:spec '{:find  [(pull tp [*])]
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

(defn delete! [trade-pattern-ids]
  (let [children   (data/query data.crux/db {:spec '{:find  [(pull tp [*])]
                                                     :where [[tp :trade-pattern/parent-id id]]
                                                     :in    [[id ...]]}
                                             :args [trade-pattern-ids]})
        del-result (data/delete! data.crux/db {:spec '{:find  [tp]
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
