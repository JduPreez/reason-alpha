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

(defn- id-label-tuple? [type]
  #?(:cljs
     (and (vector? type)
          (= (count type) 3)
          (= (-> #'string? meta :name str)
             (-> type (nth 2) str))
          (= (first type) :tuple))
     :clj
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

(defn command-ent->query-dto [query-model [command-ent & joined-ref-ents :as ents]]
  (let [all-paths-vals      (flatten-path [] command-ent)
        ref-ents-paths-vals (->> joined-ref-ents
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
            (let [;;path-template (mapv #(if (number? %) 0 %) path)
                  [nm-k
                   tuple-lbl-v] (some
                                 (fn [[k {p   :command-path
                                          pvt :pivot} type]]
                                   (let [id-lbl-tuple?   (id-label-tuple? type)
                                         [p tuple-lbl-p] (if id-lbl-tuple?
                                                           p
                                                           [p])]
                                     (when (= p path)
                                       (cond
                                         pvt
                                         , (-> path
                                               butlast
                                               vec
                                               (conj pvt)
                                               (as-> x (get all-paths-vals x))
                                               (conj []))

                                         id-lbl-tuple?
                                         , [k (get ref-ents-paths-vals tuple-lbl-p)]

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
                       [:position-creation-id {:command-path [:position/creation-id]}
                        uuid?]
                       [:position-id {:optional     true
                                      :command-path [:position/id]} uuid?]
                       [:instrument {:title        "Instrument"
                                     :ref          :instrument
                                     :command-path [[:position/instrument-id]
                                                    [:instrument/name]]}
                        [:tuple uuid? string?]]
                       [:quantity {:title        "Quantity"
                                   :command-path [:position/open-trade-transaction
                                                  :trade-transaction/quantity]}
                        float?]
                       [:open-time {:title        "Open Time"
                                    :command-path [:position/open-trade-transaction
                                                   :trade-transaction/date]}
                        inst?]
                       #_[:symbols {:optional true} string?]
                       [:open-price {:title        "Open"
                                     :command-path [:position/open-trade-transaction
                                                    :trade-transaction/price]}
                        float?]
                       [:close-price {:title        "Close"
                                      :optional     true
                                      :command-path [:position/close-trade-transaction
                                                     :trade-transaction/price]}
                        float?]
                       [:trade-pattern {:title        "Trade Pattern"
                                        :optional     true
                                        :ref          :trade-pattern
                                        :command-path [[:position/trade-pattern-id]
                                                       [:trade-pattern/name]]}
                        [:tuple uuid? string?]]]
        qry-dto       {:position-id          #uuid "017fe4f2-b562-236b-f34e-88e227dcf280"
                       :instrument           [#uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc" "111111"],
                       :quantity             "778",
                       :open-time            #inst "2022-04-02T00:00:00.000-00:00",
                       :open-price           "89789",
                       :close-price          "89789",
                       :position-creation-id #uuid "5851072d-4014-48a1-8b5d-507d10a6239b"
                       :trade-pattern        [#uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc" "Breakout"]}
        cmd-ent       [#:position{:creation-id             #uuid "5851072d-4014-48a1-8b5d-507d10a6239b",
                                  :id                      #uuid "017fe4f2-b562-236b-f34e-88e227dcf280",
                                  :instrument-id           #uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc",
                                  :open-trade-transaction
                                  #:trade-transaction{:quantity "778",
                                                      :date     #inst "2022-04-02T00:00:00.000-00:00",
                                                      :price    "89789"},
                                  :close-trade-transaction #:trade-transaction{:price "89789"},
                                  :trade-pattern-id        #uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc"}
                       #:trade-pattern{:id   #uuid "8750cf6a-0d79-45f5-9789-23e14bde3d3c"
                                       :name "Breakout"}
                       #:instrument{:id   #uuid "70f68c64-ac68-4258-bd5c-08fbe5caf3c6"
                                    :name "Starbucks"}]
        type          [:tuple uuid? string?]]
    (command-ent->query-dto qry-dto-model cmd-ent)
    #_(query-dto->command-ent qry-dto-model qry-dto))


  )
