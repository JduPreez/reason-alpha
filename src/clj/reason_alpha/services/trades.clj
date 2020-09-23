(ns reason-alpha.services.trades
  (:require [reason-alpha.data :refer [choose db]]
            [reason-alpha.data-structures :as data-structs]))

(defn get-trade-patterns []
  (let [trade-patrns (choose db [:trade-pattern/*])]
    (data-structs/conj-ancestors-path trade-patrns
                                      :trade-pattern/parent-id
                                      :trade-pattern/name
                                      :trade-pattern/id
                                      :trade-pattern/ancestors-path)))
