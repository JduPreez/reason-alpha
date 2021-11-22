(ns reason-alpha.services.trades
  (:require [reason-alpha.data :refer [query save! delete! any]]
            [reason-alpha.data.crux :as data.crux]
            [reason-alpha.data-structures :as data-structs]))

(defn get-trade-patterns []
  (let [trade-patrns (query data.crux/db
                            {:spec '{:find  [(pull tp [*])]
                                     :where [[tp :trade-pattern/id]]}})

        #_#_trade-patrns        (->> trade-patrns
                                     (filter #(nil? (:trade-pattern/parent-id %)))
                                     (map #(assoc % :trade-pattern/sub-patterns
                                                  (filter
                                                   (fn [{:keys [trade-pattern/parent-id]}]
                                                     (= parent-id (:trade-pattern/id %)))
                                                   trade-patrns))))
        #_#_with-ancestors-path (data-structs/conj-ancestors-path trade-patrns
                                                                  :trade-pattern/parent-id
                                                                  :trade-pattern/name
                                                                  :trade-pattern/id
                                                                  :trade-pattern/ancestors-path)]
    #_(clojure.pprint/pprint {::get-trade-patterns
                              (map #(if-let [pid (:trade-pattern/parent-id %)]
                                      (assoc % :trade-pattern/parent-id (java.util.UUID/fromString pid))
                                      %)
                                   trade-patrns)})
    ;; TODO: Remove map
    #_(map #(if-let [pid (:trade-pattern/parent-id %)]
            (assoc % :trade-pattern/parent-id (java.util.UUID/fromString pid))
            %)
           trade-patrns)
    trade-patrns))

(comment
  (get-trade-patterns)

  (let [trade-patrns '({:trade-pattern/name        "Two",
                        :trade-pattern/creation-id
                        #uuid "d1df1ec5-c534-4632-b75f-4127ceee5060",
                        :trade-pattern/id          #uuid "017c27c9-f904-6f8b-81b3-98ff8d59833d",
                        :trade-pattern/parent-id   #uuid "017c27c9-8426-f638-c2d6-cbb8ea6509ee",
                        :trade-pattern/description "Two is under One",
                        :crux.db/id                #uuid "017c27c9-f904-6f8b-81b3-98ff8d59833d"}
                       {:trade-pattern/name        "One",
                        :trade-pattern/creation-id
                        #uuid "7d5822bf-1800-4c5f-ae33-b1793b0558b6",
                        :trade-pattern/id          #uuid "017c27c9-8426-f638-c2d6-cbb8ea6509ee",
                        :trade-pattern/parent-id   nil,
                        :trade-pattern/description "1111111111111111",
                        :crux.db/id                #uuid "017c27c9-8426-f638-c2d6-cbb8ea6509ee"})]
    )
  
  )

(defn save-trade-pattern! [{:keys [trade-pattern/parent-id
                                   trade-pattern/id
                                   trade-pattern/creation-id]
                            :as   trade-pattern}]
  (clojure.pprint/pprint {::save-trade-pattern! trade-pattern})
  (let [tp                      (save! data.crux/db (dissoc trade-pattern
                                                            :trade-pattern/ancestors-path))
        parent-tp               (when parent-id
                                  (any data.crux/db {:spec '{:find  [(pull tp [*])]
                                                             :where [[tp :trade-pattern/id id]]
                                                             :in    [id]}
                                                     :args [parent-id]}))
        children-tp             (query data.crux/db {:spec '{:find  [(pull tp [*])]
                                                             :where [[tp :trade-pattern/parent-id id]]
                                                             :in    [id]}
                                                     :args [id]})
        tps                     (data-structs/conj-ancestors-path (cond-> [tp]
                                                                    parent-tp         (conj parent-tp)
                                                                    (seq children-tp) (into children-tp))
                                                                  :trade-pattern/parent-id
                                                                  :trade-pattern/name
                                                                  :trade-pattern/id
                                                                  :trade-pattern/ancestors-path)
        ;; Commented out, because we have to send back the parent & children, in case parent is renamed
        #_#_with-ancestors-path (some (fn [{id'          :trade-pattern/id
                                            creation-id' :trade-pattern/creation-id
                                            :as          tp'}]
                                        (when (or (= id' id)
                                                  (= creation-id' creation-id)) tp'))
                                      tps)]
    tps
    #_with-ancestors-path))

(defn delete-trade-patterns! [trade-pattern-id]
  (clojure.pprint/pprint {::delete-trade-patterns! trade-pattern-id})
  #_(let [children   (query data.crux/db {:spec '{:find  [(pull tp [*])]
                                                  :where [[tp :trade-pattern/parent-id id]]
                                                  :in    [id]}
                                          :args [trade-pattern-id]})
          del-result (delete! data.crux/db {:spec '{:find  [tp]
                                                    :where [(or [tp :trade-pattern/id id]
                                                                [tp :trade-pattern/parent-id id])]
                                                    :in    [id]}
                                            :args [trade-pattern-id]})]
    (-> children
        (as-> cdn (map #(select-keys % [:trade-pattern/id]) cdn))
        (conj {:trade-pattern/id trade-pattern-id})
        (as-> di (assoc del-result :deleted-items di)))))

(comment
  (let [id #uuid "017c0265-93a2-8d53-d2e1-ba1efc8512e0"]
    (query data.crux/db {:spec '{:find  [(pull tp [*])]
                                 :where [[tp :trade-pattern/parent-id id]]
                                 :in    [id]}
                         :args [id]}))

  (let [id #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6"]
    #_(query data.crux/db {:spec '{:find  [(pull tp [*])]
                                   :where [[tp :trade-pattern/parent-id tp-id]]
                                   :in    [tp-id]}
                           :args [id]})

    (delete-trade-pattern! id))

  (save-trade-pattern!
   {:trade-pattern/creation-id #uuid "c7057fa6-f424-4b47-b1f2-de5ae63fb5fb",
    :trade-pattern/name        "Breakout",
    :trade-pattern/description "dirt",
    :trade-pattern/user-id     #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f",
    :crux.db/id                #uuid "017b979d-f1a6-d8fe-ba75-b2f0679665e2",
    :trade-pattern/id          #uuid "017b979d-f1a6-d8fe-ba75-b2f0679665e2"})

  (save-trade-pattern!
   {:trade-pattern/name           "Breakout--",
    :trade-pattern/ancestors-path ["Breakout"],
    :trade-pattern/creation-id    nil,
    :trade-pattern/id             #uuid "0175a37a-dc1a-8a65-f12b-dfbc4aad8a27",
    :trade-pattern/parent-id      nil,
    :trade-pattern/user-id        #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f",
    :trade-pattern/description    ""})

  (save-trade-pattern!
   {:trade-pattern/name           "Buy Support or Short Resistance",
    :trade-pattern/ancestors-path ["Breakout" "Buy Support or Short Resistance"],
    :trade-pattern/creation-id    nil,
    :trade-pattern/id             #uuid "3c2d368f-aae5-4d13-a5bd-c5b340f09016",
    :trade-pattern/parent-id      #uuid "01766b52-f128-8cc1-a91b-7924783171da" ;; #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6",
    :trade-pattern/user-id        #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f",
    :trade-pattern/description    ""})
  (query db [:trade-pattern/*])
  (query db [:trade-pattern/id := #uuid "017a2a48-4694-dfcc-3e52-98e5ac8c74db"])
  )
