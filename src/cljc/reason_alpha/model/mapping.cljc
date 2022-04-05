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
      (and (sequential? obj)
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

      (and (sequential? obj)
           (not (map? obj)))
      , (conj obj v))))

#?(:cljs
   (defn- id-label-tuple? [type]
     (and (vector? type)
          (= (count type) 3)
          (= (-> #'string? meta :name str)
             (-> type (nth 2) str))
          (= (first type) :tuple))))

#?(:clj
   (defn- id-label-tuple? [type]
     (and (vector? type)
          (= (count type) 3)
          (= string?
             (-> type (nth 2)))
          (= (first type) :tuple))))

(defn query-dto->command-ent [query-model query-dto]
  (->> query-model
       rest
       seq
       (reduce
        (fn [cmd-ent [k {path  :command-path
                         pivot :pivot} type & _tail]]
          (let [v             (k query-dto)
                id-lbl-tuple? (id-label-tuple? type)
                v             (if id-lbl-tuple?
                                (first v) ;; Only assign the id - drop the label part of the tuple value
                                v)
                path          (if (and id-label-tuple?
                                       (= (count path) 2)
                                       (vector? (first path))
                                       (vector? (second path)))
                                (first path) ;; For mapping to a command entity, only use the id path - ignore the label path
                                path)
                pivot-path    (butlast path)]
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
         (map (fn [[k v]]
                (let [k (if (number? k) 0 k)]
                  (flatten-path (conj path k) v))))
         (into {}))
    [path step]))

(defn command-ent->query-dto [query-model [root-ent & other-ents :as ents]]
  (let [all-paths-vals      (flatten-path [] root-ent)
        ref-ents-paths-vals (->> other-ents
                                 (map #(flatten-path [] %))
                                 (apply merge))
        member-nm-paths     (->> query-model
                                 rest
                                 (map
                                  (fn [[k props type]]
                                    [k props type])))]
    ref-ents-paths-vals
    (->> all-paths-vals
         (reduce
          (fn [{:keys [dto membr-nm-paths] :as dto-paths} [path v]]
            (let [[nm-k
                   tuple-lbl-v] (some
                                 (fn [[k {p   :command-path
                                          pvt :pivot} type]]
                                   (let [id-lbl-tuple?   (id-label-tuple? type)
                                         [p tuple-lbl-p] (if id-lbl-tuple?
                                                           p [p])]
                                     (when (= p path)
                                       (cond
                                         pvt
                                         , (-> path
                                               butlast
                                               vec
                                               (conj pvt)
                                               (as-> x (get all-paths-vals x))
                                               (as-> x (conj [] x)))

                                         id-lbl-tuple?
                                         , [k (or (get ref-ents-paths-vals tuple-lbl-p)
                                                  "")]

                                         :else [k]))))
                                 membr-nm-paths)
                  v         (if tuple-lbl-v [v tuple-lbl-v] v)
                  dto-paths (if nm-k
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
  (let [qry-dto-model [:map
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
                        string?]
                       [:easy-equities
                        {:title        "Easy Equities",
                         :optional     true,
                         :pivot        :symbol/provider,
                         :command-path [:instrument/symbols 0 :symbol/ticker]}
                        string?]
                       [:instrument-type
                        {:title        "Type",
                         :optional     true,
                         :ref          :instrument/type,
                         :command-path [[:instrument/type] [:instrument/type-name]]}
                        [:tuple
                         keyword?
                         string?]]]
        qry-dto       {:position-id          #uuid "017fe4f2-b562-236b-f34e-88e227dcf280"
                       :instrument           [#uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc" "111111"],
                       :quantity             "778",
                       :open-time            #inst "2022-04-02T00:00:00.000-00:00",
                       :open-price           "89789",
                       :close-price          "89789",
                       :position-creation-id #uuid "5851072d-4014-48a1-8b5d-507d10a6239b"
                       :trade-pattern        [#uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc" "Breakout"]}
        cmd-ents      [[{:instrument/creation-id #uuid "ea669a4e-e815-4100-8cf3-da7d7fa50a17",
                         :instrument/name        "111111",
                         :instrument/symbols
                         [#:symbol{:ticker
                                   "4444",
                                   :provider
                                   :yahoo-finance}
                          #:symbol{:ticker
                                   "444",
                                   :provider
                                   :saxo-dma}
                          #:symbol{:ticker "4",
                                   :provider
                                   :easy-equities}],
                         :instrument/type        :share,
                         :instrument/account-id  #uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49",
                         :instrument/id          #uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc",
                         :xt/id                  #uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc"}]]
        type          [:tuple uuid? string?]]
    (command-ents->query-dtos qry-dto-model cmd-ents)
    #_(command-ent->query-dto qry-dto-model cmd-ent)
    #_(query-dto->command-ent qry-dto-model qry-dto))


  )
