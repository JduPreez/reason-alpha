(ns reason-alpha.services.trades
  (:require [reason-alpha.data :refer [choose save! db]]
            [reason-alpha.data-structures :as data-structs]))

(defn get-trade-patterns []
  (let [trade-patrns (choose db [:trade-pattern/*])]
    (data-structs/conj-ancestors-path trade-patrns
                                      :trade-pattern/parent-id
                                      :trade-pattern/name
                                      :trade-pattern/id
                                      :trade-pattern/ancestors-path)))

(defn save-trade-pattern! [{:keys [trade-pattern/parent-id
                                   trade-pattern/id]
                            :as   trade-pattern}]
  (let [tp        (save! db (dissoc trade-pattern
                                    :trade-pattern/ancestors-path))
        parent-tp (choose db [[:trade-pattern/id := parent-id]])
        tps       (data-structs/conj-ancestors-path (concat tp parent-tp)
                                                    :trade-pattern/parent-id
                                                    :trade-pattern/name
                                                    :trade-pattern/id
                                                    :trade-pattern/ancestors-path)]
    (some (fn [{id' :trade-pattern/id :as tp'}]
            (when (= id' id) tp')) tps)))

(comment

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
    :trade-pattern/parent-id      #uuid "01766b52-f128-8cc1-a91b-7924783171da"  ;; #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6",
    :trade-pattern/user-id        #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f",
    :trade-pattern/description    ""})
  (choose db [:trade-pattern/*])
  )
