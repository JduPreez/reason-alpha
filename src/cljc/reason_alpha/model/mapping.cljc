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

(defn command-ent->query-dto [schema' ents]
  (let [mega-ent            (apply merge ents)
        path-vals           (flatten-path [] mega-ent)
        gpath-vals          (->> path-vals
                                 (group-by (fn [[path _]]
                                             (mapv #(if (number? %) 0 %) path)))
                                 (map (fn [[k v]] [k (into {} v)]))
                                 (into {}))
        schema-member-paths (->> schema'
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
