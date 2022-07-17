(ns reason-alpha.model.mapping
  (:require [malli.util :as mu]
            [reason-alpha.utils :as utils]
            [sci.core :as sci]))

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
                (flatten-path (conj path k) v)))
         (into {}))
    [path step]))

(def eval-str (memoize #(sci/eval-string %)))

#_(defn- eval-str [fn-str]
  (sci/eval-string fn-str))

(defmulti path-value
  (fn [{:keys [type fun pivot _path _path-vals _arg _member-nm-key]}]
       (cond
         (id-label-tuple? type) :id-label-tuple
         pivot                  :pivot
         fun                    :function)))

(defmethod path-value :default [{:keys [path path-vals]}]
  (when-let [vs (get path-vals path)]
    {:value (-> vs vals first)}))

(defmethod path-value :id-label-tuple [{path-vals         :path-vals
                                        [path-v path-lbl] :path}]
  (let [v   (when-let [vs (get path-vals path-v)]
              (-> vs vals first))
        lbl (when-let [vs (get path-vals path-lbl)]
              (-> vs vals first))]
    {:value [v (or lbl "")]}))

(defmethod path-value :pivot [{:keys [member-nm-key pivot path path-vals] :as x}]
  (when (not= (last path) pivot)
    (let [pvt-items (-> path
                        butlast
                        vec
                        (conj pivot)
                        (as-> x (get path-vals x)))]
      (when-let [v (some (fn [[p v]]
                           (let [pvt-val (-> p
                                             butlast
                                             vec
                                             (conj pivot)
                                             (as-> x (get pvt-items x)))]
                             (when (= pvt-val member-nm-key) v)))
                         (get path-vals path))]
        {:value v}))))

(defmethod path-value :function [{:keys [fun arg path path-vals]}]
  (let [val-k (last path)]
    (when (not= val-k arg)
      (let [f        (-> fun str eval-str)
            arg-vals (-> path
                         butlast
                         vec
                         (conj arg)
                         (as-> x (get path-vals x)))]
        (some (fn [[p v]]
                (let [arg-v (-> p
                                butlast
                                vec
                                (conj arg)
                                (as-> x (get arg-vals x)))]
                  (f {arg   arg-v
                      val-k v})))
              (get path-vals path))))))

;; (defn command-ent->query-dto [query-model [root-ent & other-ents :as ents]]
;;   (let [all-paths-vals      (flatten-path [] root-ent)
;;         ref-ents-paths-vals (->> other-ents
;;                                  (map #(flatten-path [] %))
;;                                  (apply merge))
;;         member-nm-paths     (->> query-model
;;                                  rest
;;                                  (map
;;                                   (fn [[k props type]]
;;                                     [k props type])))]
;;     (->> all-paths-vals
;;          (reduce
;;           (fn [{:keys [dto membr-nm-paths] :as dto-paths} [path v]]
;;             (let [path-template (mapv #(if (number? %) 0 %) path)
;;                   [nm-k
;;                    tuple-lbl-v] (some
;;                                  (fn [[k {p   :command-path
;;                                           pvt :pivot} type]]
;;                                    (let [id-lbl-tuple?   (id-label-tuple? type)
;;                                          [p tuple-lbl-p] (if id-lbl-tuple?
;;                                                            p [p])]
;;                                      (when (= p path-template)
;;                                        (cond
;;                                          pvt
;;                                          , (-> path
;;                                                butlast
;;                                                vec
;;                                                (conj pvt)
;;                                                (as-> x (get all-paths-vals x))
;;                                                (as-> x (conj [] x)))

;;                                          id-lbl-tuple?
;;                                          , [k (or (get ref-ents-paths-vals tuple-lbl-p)
;;                                                   "")]

;;                                          :else [k]))))
;;                                  membr-nm-paths)
;;                   v         (if tuple-lbl-v [v tuple-lbl-v] v)
;;                   dto-paths (if nm-k
;;                               {:dto            (assoc dto nm-k v)
;;                                :membr-nm-paths (remove
;;                                                 (fn [[k _]]
;;                                                   (= k nm-k))
;;                                                 membr-nm-paths)}
;;                               dto-paths)]
;;               dto-paths))
;;           {:dto            {}
;;            :membr-nm-paths member-nm-paths})
;;          :dto)))

(defn command-ent->query-dto [query-model ents]
  (let [mega-ent            (apply merge ents)
        path-vals           (flatten-path [] mega-ent)
        gpath-vals          (->> path-vals
                                 (group-by (fn [[path _]]
                                             (mapv #(if (number? %) 0 %) path)))
                                 (map (fn [[k v]] [k (into {} v)]))
                                 (into {}))
        schema-member-paths (->> query-model
                                 rest
                                 (mapcat
                                  (fn [[_ {:keys         [command-path pivot]
                                           {:keys [arg]} :fn-value} type :as schema-membr]]
                                    (let [arg-path (when (and command-path arg)
                                                     [(-> command-path
                                                          butlast
                                                          vec
                                                          (conj arg)) schema-membr])
                                          pvt-path (when (and command-path pivot)
                                                     [(-> command-path
                                                          butlast
                                                          vec
                                                          (conj pivot)) schema-membr])
                                          cmd-path (when (and command-path
                                                              (not arg-path))
                                                     ;; Check if it's a tuple
                                                     (if (every? sequential? command-path)
                                                       [(first command-path) schema-membr]
                                                       [command-path schema-membr]))]
                                      (cond-> []
                                        cmd-path (conj cmd-path)
                                        arg-path (conj arg-path)
                                        pvt-path (conj pvt-path)))))
                                 (group-by first)
                                 (map (fn [[k v]]
                                        [k (map second v)]))
                                 (into {}))
        dto                 (->> schema-member-paths
                                 keys
                                 (select-keys gpath-vals)
                                 (reduce
                                  (fn [dto [path sub-path-vals]]
                                    (let [kvs (->> (for [smp  (get schema-member-paths path)
                                                         :let [[member-nm-key
                                                                {p                 :command-path
                                                                 pvt               :pivot
                                                                 {:keys [arg fun]} :fn-value}
                                                                type] smp
                                                               v (path-value {:type          type
                                                                              :path          p
                                                                              :path-vals     gpath-vals
                                                                              :fun           fun
                                                                              :arg           arg
                                                                              :pivot         pvt
                                                                              :member-nm-key member-nm-key})]]
                                                     (when v {member-nm-key (:value v)}))
                                                   (remove nil?))]
                                      (apply merge dto kvs)))
                                  {}))]
    dto))

(defn command-ents->query-dtos [query-model command-ents]
  (map #(command-ent->query-dto query-model %)
       command-ents))


(comment

  (let [query-model         [:map
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
                               :command-path [[:instrument/type]
                                              [:instrument/type-name]]}
                              [:tuple keyword string?]]
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
                             [:currency-account
                              {:title        "Currency Account"
                               :optional     true
                               :command-path [:instrument/currency-instrument :instrument/account :account/user-name]}]]
        ents                [{:instrument/id   #uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc",
                              :instrument/creation-id
                              #uuid "ea669a4e-e815-4100-8cf3-da7d7fa50a17",
                              :instrument/name "Starbucks",
                              :instrument/symbols
                              [{:symbol/ticker   "SBUX"
                                :symbol/provider :yahoo-finance}
                               {:symbol/ticker   "SBUX.NY"
                                :symbol/provider :easy-equities}],
                              :instrument/type :share}]
        mega-ent            (apply merge ents)
        path-vals           (flatten-path [] mega-ent)
        gpath-vals          (->> path-vals
                                 (group-by (fn [[path _]]
                                             (mapv #(if (number? %) 0 %) path)))
                                 (map (fn [[k v]] [k (into {} v)]))
                                 (into {}))
        schema-member-paths (->> query-model
                                 rest
                                 (mapcat
                                  (fn [[_ {:keys         [command-path pivot]
                                           {:keys [arg]} :fn-value} type :as schema-membr]]
                                    (let [arg-path (when (and command-path arg)
                                                     [(-> command-path
                                                          butlast
                                                          vec
                                                          (conj arg)) schema-membr])
                                          pvt-path (when (and command-path pivot)
                                                     [(-> command-path
                                                          butlast
                                                          vec
                                                          (conj pivot)) schema-membr])
                                          cmd-path (when (and command-path
                                                              (not arg-path))
                                                     ;; Check if it's a tuple
                                                     (if (every? sequential? command-path)
                                                       [(first command-path) schema-membr]
                                                       [command-path schema-membr]))]
                                      (cond-> []
                                        cmd-path (conj cmd-path)
                                        arg-path (conj arg-path)
                                        pvt-path (conj pvt-path)))))
                                 (group-by first)
                                 (map (fn [[k v]]
                                        [k (map second v)]))
                                 (into {}))
        #_#_gpath-vals      (select-keys gpath-vals (keys schema-member-paths))
        dto                 (->> schema-member-paths
                                 keys
                                 (select-keys gpath-vals)
                                 (reduce
                                  (fn [dto [path sub-path-vals]]
                                    (let [kvs (->> (for [smp  (get schema-member-paths path)
                                                         :let [[member-nm-key
                                                                {p                 :command-path
                                                                 pvt               :pivot
                                                                 {:keys [arg fun]} :fn-value}
                                                                type] smp
                                                               v (path-value {:type          type
                                                                              :path          p
                                                                              :path-vals     gpath-vals
                                                                              :fun           fun
                                                                              :arg           arg
                                                                              :pivot         pvt
                                                                              :member-nm-key member-nm-key})]]
                                                     (when v {member-nm-key (:value v)}))
                                                   (remove nil?))]
                                      (apply merge dto kvs)))
                                  {}))]
    ;;schema-member-paths
    dto)

  (let [[k v :as x] nil] x)


  (let [qry-dto-model [:map
                       [:position-creation-id {:command-path [:position/creation-id]}
                        uuid?]
                       [:position-id {:optional     true
                                      :command-path [:position/id]} uuid?]
                       [:holding {:title        "Holding (Instrument)"
                                  :ref          :holding
                                  :command-path [[:position/holding-id]
                                                 [:holding/instrument-name]]}
                        [:tuple uuid? string?]]
                       [:quantity {:title        "Quantity"
                                   :command-path [:position/open
                                                  :trade-transaction/quantity]}
                        number?]
                       [:long-short {:title        "Long/Short (Hedge)"
                                     :ref          :position/long-short
                                     :command-path [[:position/long-short]
                                                    [:position/long-short-name]]}
                        [:tuple keyword? string?]]
                       [:open-time {:title        "Open Time"
                                    :command-path [:position/open
                                                   :trade-transaction/date]}
                        inst?]
                       [:open-price {:title        "Open"
                                     :command-path [:position/open
                                                    :trade-transaction/price]}
                        number?]
                       [:close-price {:title        "Close"
                                      :optional     true
                                      :command-path [:position/close
                                                     :trade-transaction/price]}
                        number?]
                       [:status {:optional     true
                                 :command-path [:position/status]}
                        keyword?]
                       [:stop {:optional     true
                               :title        "Stop"
                               :command-path [:position/stop]}
                        number?]
                       [:trade-pattern {:title        "Trade Pattern"
                                        :optional     true
                                        :ref          :trade-pattern
                                        :command-path [[:position/trade-pattern-id]
                                                       [:trade-pattern/name]]}
                        [:tuple uuid? string?]]
                       [:holding-position-id {:title        "Holding Position"
                                              :optional     true
                                              :ref          :position/holding-position
                                              :command-path [:position/holding-position-id]}
                        uuid?]
                       [:stop-loss {:title    "Stop Total Loss"
                                    :optional true}
                        float?]
                       [:eod-historical-data {:optional     true
                                              :fn-value     {:arg :symbol/provider
                                                             :fun '(fn [{p :symbol/provider} v]
                                                                     (when (= p :eod-historical-data)
                                                                       {:value v}))}
                                              :command-path [:holding/symbols 0 :symbol/ticker]}
                        string?]]
        qry-dto       {:position-id          #uuid "017fe4f2-b562-236b-f34e-88e227dcf280"
                       :instrument           [#uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc" "111111"],
                       :quantity             "778",
                       :open-time            #inst "2022-04-02T00:00:00.000-00:00",
                       :open-price           "89789",
                       :close-price          "89789",
                       :position-creation-id #uuid "5851072d-4014-48a1-8b5d-507d10a6239b"
                       :trade-pattern        [#uuid "017fd139-a0bd-d2b4-11f2-222a61e7edfc" "Breakout"]}
        cmd-ents      #{[{:position/long-short  :long,
                          :position/id          #uuid "01808fb9-4f61-999f-6a3b-58f29b27acee",
                          :position/status      :open,
                          :position/creation-id #uuid "bdc2bfb6-dcd4-4674-bc91-dc56b0a93276",
                          :position/open
                          #:trade-transaction{:quantity 22.44,
                                              :date     #inst "2022-05-12T00:00:00.000-00:00",
                                              :price    23,
                                              :type     :buy,
                                              :holding-id
                                              #uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5"},
                          :position/close
                          #:trade-transaction{:price    33.3,
                                              :type     :sell,
                                              :holding-id
                                              #uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5",
                                              :quantity 22.44},
                          :position/holding-id  #uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5",
                          :xt/id                #uuid "01808fb9-4f61-999f-6a3b-58f29b27acee",
                          :position/account-id  #uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49",
                          :position/stop        3.2}
                         {:holding/creation-id     #uuid "e4c5d7d8-5abe-4182-942b-f6c04e2b1c0c",
                          :holding/instrument-name "AAA",
                          :holding/symbols
                          [#:symbol{:ticker "AAA", :provider :yahoo-finance}
                           #:symbol{:ticker "AAA", :provider :saxo-dma}
                           #:symbol{:ticker "AAA", :provider :easy-equities}],
                          :holding/instrument-type :share,
                          :holding/account-id      #uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49",
                          :holding/id              #uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5",
                          :xt/id                   #uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5"}
                         nil]
                        [{:position/holding-position-id #uuid "018008e1-a0db-2638-b2e2-4e9f0e332d11",
                          :position/trade-pattern-id    #uuid "01800865-9069-63c7-9c6c-4d24cdcefc9a",
                          :position/long-short          :long,
                          :position/id                  #uuid "0180098b-e65e-d7ce-645b-41eef737fa0c",
                          :position/creation-id         #uuid "74e921b2-fe79-454d-b47c-08c43b298019",
                          :position/open
                          #:trade-transaction{:quantity "3333",
                                              :date     #inst "2022-04-06T00:00:00.000-00:00",
                                              :price    "333"},
                          :position/close               #:trade-transaction{:price 36.85, :estimated? true},
                          :position/holding-id          #uuid "018004bb-227c-d6de-69ac-bc1eab688ab5",
                          :xt/id                        #uuid "0180098b-e65e-d7ce-645b-41eef737fa0c",
                          :position/account-id          #uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49",
                          :position/stop                "3333"}
                         {:holding/creation-id     #uuid "c818b4ab-fbc7-48ca-ab7b-abf15a71b74c",
                          :holding/instrument-name "BBB",
                          :holding/symbols
                          [#:symbol{:ticker "BBB", :provider :yahoo-finance}
                           #:symbol{:ticker "BBB", :provider :saxo-dma}
                           #:symbol{:ticker "BBB", :provider :easy-equities}],
                          :holding/instrument-type :crypto,
                          :holding/account-id      #uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49",
                          :holding/id              #uuid "018004bb-227c-d6de-69ac-bc1eab688ab5",
                          :xt/id                   #uuid "018004bb-227c-d6de-69ac-bc1eab688ab5"}
                         {:trade-pattern/creation-id #uuid "47427389-60f3-4f0b-a32e-7fbe139b6e36",
                          :trade-pattern/id          #uuid "01800865-9069-63c7-9c6c-4d24cdcefc9a",
                          :trade-pattern/name        "Breakout",
                          :trade-pattern/description "Breakout",
                          :trade-pattern/account-id  #uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49",
                          :xt/id                     #uuid "01800865-9069-63c7-9c6c-4d24cdcefc9a"}]
                        #_[{:position/trade-pattern-id #uuid "0180088d-aa18-6709-de16-4d2e56126947",
                            :position/long-short       :long,
                            :position/id               #uuid "018008e1-a0db-2638-b2e2-4e9f0e332d11",
                            :position/creation-id      #uuid "0d4a7fdf-5ab0-4d08-a35a-c5b23fa46c6e",
                            :position/open
                            #:trade-transaction{:quantity "454",
                                                :date     #inst "2022-04-20T00:00:00.000-00:00",
                                                :price    "23"},
                            :position/close            #:trade-transaction{:price 83.33, :estimated? true},
                            :position/holding-id       #uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5",
                            :xt/id                     #uuid "018008e1-a0db-2638-b2e2-4e9f0e332d11",
                            :position/account-id       #uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49",
                            :position/stop             "2342"}
                           {:holding/creation-id     #uuid "e4c5d7d8-5abe-4182-942b-f6c04e2b1c0c",
                            :holding/instrument-name "AAA",
                            :holding/symbols
                            [#:symbol{:ticker "AAA", :provider :yahoo-finance}
                             #:symbol{:ticker "AAA", :provider :saxo-dma}
                             #:symbol{:ticker "AAA", :provider :easy-equities}],
                            :holding/instrument-type :share,
                            :holding/account-id      #uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49",
                            :holding/id              #uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5",
                            :xt/id                   #uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5"}
                           {:trade-pattern/creation-id #uuid "1d9cd550-017a-4cbb-8fe2-38db3971d394",
                            :trade-pattern/id          #uuid "0180088d-aa18-6709-de16-4d2e56126947",
                            :trade-pattern/parent-id   #uuid "01800865-9069-63c7-9c6c-4d24cdcefc9a",
                            :trade-pattern/name        "zzzzzz",
                            :trade-pattern/description "zzzz",
                            :trade-pattern/account-id  #uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49",
                            :xt/id                     #uuid "0180088d-aa18-6709-de16-4d2e56126947"}]}]
    (command-ents->query-dtos qry-dto-model cmd-ents))


  )
