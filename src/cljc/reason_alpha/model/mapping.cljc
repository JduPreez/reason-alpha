(ns reason-alpha.model.mapping
  (:require [malli.util :as mu]
            [reason-alpha.utils :as utils]))

(def full-model [:map
                       [:instrument-id
                        {:optional true, :command-path [:instrument/id]}
                        uuid?]
                       [:instrument-creation-id
                        {:command-path [:instrument/creation-id]}
                        uuid?]
                       [:instrument-name
                        {:title        "Instrument",
                         :optional     true,
                         :command-path [:instrument/name]}
                        string?]
                       [:symbol/yahoo-finance
                        {:title    "Yahoo! Finance",
                         :optional true,
                         :command-path
                         [:instrument/symbols
                          [:symbol/ticker #:symbol{:provider :yahoo-finance}]]}
                        string?]
                       [:symbol/saxo-dma
                        {:title    "Saxo/DMA",
                         :optional true,
                         :command-path
                         [:instrument/symbols
                          [:symbol/ticker #:symbol{:provider :saxo-dma}]]}
                        string?]
                       [:symbol/easy-equities
                        {:title    "Easy Equities",
                         :optional true,
                         :command-path
                         [:instrument/symbols
                          [:symbol/ticker #:symbol{:provider :easy-equities}]]}
                        string?]
                       [:instrument-type
                        {:title        "Type",
                         :optional     true,
                         :ref          :instrument/type,
                         :command-path [:instrument/type]}
                        keyword?]])

(defn mv-assoc-in
  [obj [k & ks] v]
  #_(clojure.pprint/pprint {:obj obj
                          :k   k
                          :ks  ks
                          :v   v})
  (if ks
    (cond
      (map? obj)
      , (assoc obj k (mv-assoc-in (get obj k
                                       (if (keyword? (first ks))
                                         {} []))
                                  ks v))
      (and (coll? obj)
           (not (map? obj)))
      , (conj obj (mv-assoc-in (get obj k
                                    (if (keyword? (first ks))
                                      {} []))
                               ks v)))
    (cond
      (and (nil? k) (map? obj) (map? v))
      , (merge obj v)

      (map? obj)
      , (assoc obj k v)

      (and (coll? obj)
           (not (map? obj)))
      , (conj obj v))))

(defn query-dao->command-ent [query-model query-dao]
  (->> query-model
       rest
       seq
       (reduce
        (fn [cmd-ent [k {path  :command-path
                         pivot :pivot} & tail]]
          (let [v          (k query-dao)
                pivot-path (butlast path)]
            (if (and path v)
              (if pivot
                (mv-assoc-in cmd-ent pivot-path {(last path) v
                                                 pivot       k})
                (mv-assoc-in cmd-ent path v))
              cmd-ent))) {})))

(comment
  (letfn []
    (let [#_#_command-ent-model [:map
                                 [:instrument/creation-id uuid?]
                                 [:instrument/id {:optional true} uuid?]
                                 [:instrument/name [:string {:min 1}]]
                                 [:instrument/symbols {:optional true}
                                  [:sequential
                                   [:map
                                    [:symbol/ticker {:min 1} string?]
                                    [:symbol/instrument-id {:optional true} uuid?]
                                    [:symbol/provider
                                     [:enum {:enum/titles {:yahoo-finance "Yahoo! Finance"
                                                           :saxo-dma      "Saxo/DMA"
                                                           :easy-equities "Easy Equities"}}
                                      :yahoo-finance :saxo-dma :easy-equities]]]]]
                                 [:instrument/type [:enum
                                                    {:enum/titles {:share    "Share"
                                                                   :etf      "ETF"
                                                                   :currency "Currency"
                                                                   :crypto   "Crypto"}}
                                                    :share :etf :currency :crypto]]
                                 [:instrument/currency-instrument-id uuid?]
                                 [:instrument/account-id uuid?]]
          query-dao-model       [:map
                             [:instrument-id
                              {:optional true, :command-path [:instrument/id]}
                              uuid?]
                             [:instrument-creation-id
                              {:command-path [:instrument/creation-id]}
                              uuid?]
                             [:instrument-name
                              {:title        "Instrument",
                               :optional     true,
                               :command-path [:instrument/name]}
                              string?]
                             [:instrument-type
                              {:title        "Type",
                               :optional     true,
                               :ref          :instrument/type,
                               :command-path [:instrument/type]}
                              keyword?]
                             [:yahoo-finance
                              {:title        "Yahoo! Finance",
                               :optional     true,
                               :pivot        :symbol/provider,
                               :command-path [:instrument/symbols 0 :symbol/ticker]}
                              string?]
                             [:saxo-dma
                              {:title        "Saxo/DMA",
                               :optional     true,
                               :pivot        :symbol/provider,
                               :command-path [:instrument/symbols 0 :symbol/ticker]}
                              string?]]
          query-dao             {:instrument-id          (utils/new-uuid)
                                 :instrument-creation-id (utils/new-uuid)
                                 :instrument-type        :share
                                 :saxo-dma               "00700:xhkg"
                                 :yahoo-finance          "0700.hk"}
          #_#_upd1              (mv-assoc-in {} [:instrument/symbols 0 :symbol/ticker] "tikr-1")
          #_#_upd2              (mv-assoc-in upd1 [:instrument/symbols 0] {:symbol/provider :saxo-dma
                                                                           :symbol/ticker   "tikr-2"})
          #_#_upd3              (mv-assoc-in upd2 [:instrument/id] (utils/new-uuid))]
      (query-dao->command-ent query-dao-model query-dao)))

  (update-in {:instrument/symbols [{:symbol/ticker "---"}]} [:instrument/symbols 0 :symbol/ticker] (constantly "dddd"))
  (update-in {:name "James" :age 26} [:age] inc)
  )
