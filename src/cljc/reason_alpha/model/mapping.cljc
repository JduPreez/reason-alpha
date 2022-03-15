(ns reason-alpha.model.mapping
  (:require [malli.util :as mu]
            [reason-alpha.utils :as utils]))

(defn mv-assoc-in
  "A version of `assoc-in` (`mv-` = map & vector) that creates nested maps & collections."
  [obj [k & ks] v]
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

(defn query-dto->command-ent [query-model query-dto]
  (->> query-model
       rest
       seq
       (reduce
        (fn [cmd-ent [k {path  :command-path
                         pivot :pivot} & _tail]]
          (let [v          (k query-dto)
                pivot-path (butlast path)]
            (if (and path v)
              (if pivot
                (mv-assoc-in cmd-ent pivot-path {(last path) v
                                                 pivot       k})
                (mv-assoc-in cmd-ent path v))
              cmd-ent))) {})))

(defn- to-indexed-seqs [coll]
  (if (map? coll)
    coll
    (map vector (range) coll)))

(defn- flatten-path [path step]
  (if (coll? step)
    (->> step
         to-indexed-seqs
         (map (fn [[k v]] (flatten-path (conj path k) v)))
         (into {}))
    [path step]))

(defn command-ent->query-dto [query-model command-ent]
  (let [all-paths-vals  (flatten-path [] command-ent)
        member-nm-paths (->> query-model
                             rest
                             (map
                              (fn [[k props]]
                                [k props])))]
    (->> all-paths-vals
         (reduce
          (fn [{:keys [dto membr-nm-paths] :as dto-paths} [path v]]
            (let [path-template (mapv #(if (number? %) 0 %) path)
                  nm-k          (some
                                 (fn [[k {p   :command-path
                                          pvt :pivot}]]
                                   (when (= p path-template)
                                     (if pvt
                                       (-> path
                                           butlast
                                           vec
                                           (conj pvt)
                                           (as-> x (get all-paths-vals x)))
                                       k)))
                                 membr-nm-paths)
                  dto-paths     (if nm-k
                                  {:dto            (assoc dto nm-k v)
                                   :membr-nm-paths (remove
                                                    (fn [[k _]]
                                                      (= k nm-k))
                                                    membr-nm-paths)}
                                  dto-paths)]
              dto-paths))
          {:dto            {}
           :membr-nm-paths member-nm-paths})
         :dto)))

(defn command-ents->query-dtos [query-model command-ents]
  (map #(command-ent->query-dto query-model %)
       command-ents))


(comment
  (letfn []
    (let [query-dto-model [:map
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
          query-dto       {:instrument-id          (utils/new-uuid)
                           :instrument-creation-id (utils/new-uuid)
                           :instrument-type        :share
                           :saxo-dma               "00700:xhkg"
                           :yahoo-finance          "0700.hk"}
          command-ent     {:instrument/id
                           #uuid "c16bb005-3d13-45da-9a5c-3e024c1445bf"
                           :instrument/creation-id
                           #uuid "74147a7a-07e8-4947-98b0-293f2d9766e2",
                           :instrument/name "jkjkjkj",
                           :instrument/symbols
                           [{:symbol/ticker "YF", :symbol/provider :yahoo-finance}
                            {:symbol/ticker "SDMA", :symbol/provider :saxo-dma}
                            {:symbol/ticker "EE", :symbol/provider :easy-equities}],
                           :instrument/type :crypto}
          #_#_upd1        (mv-assoc-in {} [:instrument/symbols 0 :symbol/ticker] "tikr-1")
          #_#_upd2        (mv-assoc-in upd1 [:instrument/symbols 0] {:symbol/provider :saxo-dma
                                                                     :symbol/ticker   "tikr-2"})
          #_#_upd3        (mv-assoc-in upd2 [:instrument/id] (utils/new-uuid))]
      (command-ent->query-dto query-dto-model command-ent)))


  (update-in {:instrument/symbols [{:symbol/ticker "---"}]} [:instrument/symbols 0 :symbol/ticker] (constantly "dddd"))
  (update-in {:name "James" :age 26} [:age] inc)

;; [:map
;;                                  [:instrument/creation-id uuid?]
;;                                  [:instrument/id {:optional true} uuid?]
;;                                  [:instrument/name [:string {:min 1}]]
;;                                  [:instrument/symbols {:optional true}
;;                                   [:sequential
;;                                    [:map
;;                                     [:symbol/ticker {:min 1} string?]
;;                                     [:symbol/instrument-id {:optional true} uuid?]
;;                                     [:symbol/provider
;;                                      [:enum {:enum/titles {:yahoo-finance "Yahoo! Finance"
;;                                                            :saxo-dma      "Saxo/DMA"
;;                                                            :easy-equities "Easy Equities"}}
;;                                       :yahoo-finance :saxo-dma :easy-equities]]]]]
;;                                  [:instrument/type [:enum
;;                                                     {:enum/titles {:share    "Share"
;;                                                                    :etf      "ETF"
;;                                                                    :currency "Currency"
;;                                                                    :crypto   "Crypto"}}
;;                                                     :share :etf :currency :crypto]]
;;                                  [:instrument/currency-instrument-id uuid?]
;;                                  [:instrument/account-id uuid?]]


  )
