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

(defn save-trade-pattern! [{:keys [trade-pattern/ancestors-path]
                            :as   trade-pattern}]
  (as-> (dissoc trade-pattern
              :trade-pattern/ancestors-path) tp
    (save! db tp)
    (first tp)
    (assoc tp :trade-pattern/ancestors-path ancestors-path)))

(comment

  (save-trade-pattern!
   {:trade-pattern/name           "Breakout--",
    :trade-pattern/ancestors-path ["Breakout"],
    :trade-pattern/creation-id    nil,
    :trade-pattern/id             #uuid "0175a37a-dc1a-8a65-f12b-dfbc4aad8a27",
    :trade-pattern/parent-id      nil,
    :trade-pattern/user-id        #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f",
    :trade-pattern/description    ""})

  )
