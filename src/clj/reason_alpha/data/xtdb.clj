(ns reason-alpha.data.xtdb
"Some concepts about this Crux DB namespace:
  - Entity: Is a thing expressed through a map, with a unique identity.
  - Identity: An entity's identity consists of two namespaced UUID keys with the
    name `<entity-type>/id` and `<entity-type>/creation-id`.
    Any entity map is only allowed to contain one of each one of these keys.
  - `<entity-type>/creation-id`: Is a temporary UUID assigned by the client system where
    the entity originated for a new entity that hasn't been saved to the DB.
    This is only  guaranteed to be unique in the context of the client system, and not globally.
  - `<entity-type>/id`: Is the official global ID of the entity after it was saved to the DB.
  - References to other entities: All references to other entities ('foreign keys') consists of
    a non-namespaced keyword `<referenced-entity-type>` that can be the referenced entity's
    map or it's ID UUID.
  Therefore an entity will always have a `creation-id`, but not necessarily an `id`, because the
  entity will only be assigned it's globally unique `id` after being saved to the DB.
  Therefore to identify what a map's entity type is we just have to find the namespace of the
  `creation-id` key."
  (:import [com.github.f4b6a3.uuid UuidCreator])
  (:require [clojure.java.io :as io]
            [xtdb.api :as xt]
            [me.raynes.fs :as fs]
            [outpace.config :refer [defconfig]]
            [reason-alpha.data.model :as data.model :refer [DataBase]]
            [reason-alpha.model.core :as model]))

(defconfig data-dir) ;; "data"
(defconfig db-name) ;; "dev"

(defn drop-db! [db]
  (fs/delete-dir (str data-dir "/" db)))

(defn- put-delete-fn!
  [{node                  :node
    only-when-not-exists? :only-when-not-exists?
    :or                   {only-when-not-exists? true}}]
  (let [already-exists? (-> (xt/db node)
                            (xt/q  '{:find  [(count fun)]
                                     :where [[del-tx-fn :xt/fn fun]
                                             [del-tx-fn :xt/id ::delete]]})
                            first
                            first
                            (= 0))]
    (when (or (and only-when-not-exists?
                   (false? already-exists?))
              (false? only-when-not-exists?))
      (xt/submit-tx node
                    [[::xt/put
                      {:xt/id ::delete
                       :xt/fn
                       , '(fn [ctx {:keys [spec args]}]
                            (->> args
                                 (apply xtdb.api/q
                                        (xtdb.api/db ctx)
                                        spec)
                                 (map #(let [doc (first %)
                                             id  (if (map? doc)
                                                   (-> % first vals first)
                                                   doc)]
                                         [::xt/delete id]))
                                 vec))}]]))))

(defn xtdb-start! []
  (clojure.pprint/pprint ::xtdb-start!)
  (let [fn-kv-store (fn [dir]
                      {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                                  :db-dir      (-> data-dir
                                                   (str "/" db-name "/" dir)
                                                   io/file)
                                  :sync?       true}})
        node        (xt/start-node
                     {:xtdb/tx-log         (fn-kv-store "tx-log")
                      :xtdb/document-store (fn-kv-store "doc-store")
                      :xtdb/index-store    (fn-kv-store "index-store")})]
    (put-delete-fn! {:node node})
    node))

(defn- maybe-add-id [entities]
  (->> entities
       (map #(let [id-key (model/entity-id-key %)]
               (-> %
                   id-key
                   (or (UuidCreator/getLexicalOrderGuid))
                   (as-> idv (assoc % :xt/id idv))
                   (as-> ent (assoc ent id-key (:xt/id ent))))))))

(defn- xtdb-puts [entities]
  (->> entities
       (map (fn [ent] [::xt/put ent]))
       vec))

(defn xtdb-save! [*db-node entities]
  (let [ents-with-ids (maybe-add-id entities)]
    (xt/submit-tx @*db-node (xtdb-puts ents-with-ids))
    ents-with-ids))

(defn xtdb-delete! [*db-node {:keys [spec] :as del-command}]
  (let [del-cmd            (update del-command
                                   :args
                                   (fn [a]
                                     (mapv #(if (instance? clojure.lang.IObj
                                                           %)
                                              (vary-meta % (fn [_] nil))
                                              %) a)))
        {:keys [xt/tx-id]
         :as   tx-details} (cond
                             spec
                             , (xt/submit-tx @*db-node
                                             [[::xt/fn
                                               ::delete
                                               del-cmd]])

                             del-command
                             , (xt/submit-tx @*db-node
                                             del-cmd)

                             :else {:was-deleted? false})]
    {:tx-details   tx-details
     :was-deleted? (not (nil? tx-id))}))

(deftype XTDB [*db-node fn-save!
               fn-delete! fn-start-db!]
  DataBase
  (disconnect [_]
    (.close @*db-node))

  (connect [_]
    (println "db-node start " @*db-node)
    (when @*db-node
      (.close @*db-node))
    (reset! *db-node (fn-start-db!))
    (println "db-node end" @*db-node)
    @*db-node)

  (query [_ {:keys [spec args]}]
    (->> args
         (mapv #(if (instance? clojure.lang.IObj
                               %)
                  (vary-meta % (fn [_] nil))
                  %))
         (apply xt/q (xt/db @*db-node) spec)
         (map (fn [[entity :as all]]
                (if (map? entity)
                  entity
                  all)))))

  (any [this query-spec]
    (first (.query this query-spec)))

  ;; Delete command's spec should only return :crux.db/id
  (delete! [this delete-command]
    (fn-delete! *db-node delete-command))

  (save! [this entity]
    (first (fn-save! *db-node [entity])))

  (add-all! [this entities]
    (fn-save! *db-node entities)))

(def db
  (XTDB. (atom nil) xtdb-save! xtdb-delete! xtdb-start!))

(comment


  (require '[reason-alpha.model.mapping :as mapping]
           '[reason-alpha.model.fin-instruments :as fin-instruments])

  (def n (data.model/connect db))

  (data.model/disconnect db)

  (let [account-id        #uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49"
        trade-pattern-ids (with-meta [#uuid "017efdef-f2ea-4a0f-494a-df7ef82b8ab2"]
                            {:something #(-> %
                                             (str "/")
                                             io/file)})
        args              (->> [trade-pattern-ids]
                               (mapv #(if (instance? clojure.lang.IObj
                                                     %)
                                        (vary-meta % (fn [_] nil))
                                        %)))
        spec              '{:find  [e]
                            :where [(or (and [e :trade-pattern/id id]
                                             [e :trade-pattern/account-id acc-id])
                                        (and [e :trade-pattern/parent-id id]
                                             [e :trade-pattern/account-id acc-id]))]
                            :in    [acc-id [id ...]]}]
    (xt/submit-tx n [[::xt/fn
                      ::delete
                      {:spec '{:find  [e]
                               :where [[e :trade-pattern/id id]]
                               :in    [[id ...]]}
                       :args args}]])
    )

  (data.model/query db
                    {:spec '{:find  [(pull tp [*])]
                             :where [[tp :trade-pattern/id]
                                     #_[tp :trade-pattern/name "Breakout"]]}})

(xt/submit-tx n
              [[::xt/put
                {:xt/id ::delete
                 :xt/fn
                 , '(fn [ctx {:keys [spec args]}]
                      (->> args
                           (apply xtdb.api/q
                                  (xtdb.api/db ctx)
                                  spec)
                           (map #(let [doc (first %)
                                       id  (if (map? doc)
                                             (-> % first vals first)
                                             doc)]
                                   [::xt/delete id]))
                           vec))}]])
 
(xt/q (xt/db n) '{:find  [fun]
                  :where [[del-tx-fn :xt/fn fun]
                          [del-tx-fn :xt/id ::delete]]})


  



  (xt/submit-tx n
                [[::xt/put
                  {:xt/id ::delete
                   :xt/fn
                   '(fn [ctx {:keys [spec args]}]
                      (spit "/home/jacques/Proj/reason-alpha/xtdb1.log"
                            (pr-str {:spec spec
                                     :args args
                                     :op   (->> args
                                                (apply xtdb.api/q
                                                       (xtdb.api/db ctx)
                                                       spec)
                                                (map #(let [doc (first %)
                                                            id  (if (map? doc)
                                                                  (-> % first vals first)
                                                                  doc)]
                                                        [::xt/delete id]))
                                                vec)}))
                      (->> args
                           (apply xtdb.api/q
                                  (xtdb.api/db ctx)
                                  spec)
                           (map #(let [doc (first %)
                                       id  (if (map? doc)
                                             (-> % first vals first)
                                             doc)]
                                   [::xt/delete id]))
                           vec))}]])

  

  (data.model/delete! db #uuid "017f92a5-ff38-70db-a7c9-0bc5c0fbc95b" #_{:spec '{:find  [e]
                                                                                 :where [(or (and [e :trade-pattern/id id]
                                                                                                  [e :trade-pattern/account-id acc-id])
                                                                                             (and [e :trade-pattern/parent-id id]
                                                                                                  [e :trade-pattern/account-id acc-id]))]
                                                                                 :in    [acc-id [id ...]]}
                                                                         :args [account-id trade-pattern-ids]})

  (data.model/query db {:spec
                        '{:find [e],
                          :where
                          [(or
                            (and [e :trade-pattern/id id] [e :trade-pattern/account-id acc-id])
                            (and
                             [e :trade-pattern/parent-id id]
                             [e :trade-pattern/account-id acc-id]))],
                          :in   [acc-id [id ...]]},
                        :args
                        [#uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49"
                         '(#uuid "017f92a5-ff38-70db-a7c9-0bc5c0fbc95b")]})
  )
