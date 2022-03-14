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
                         , '(fn [ctx {:keys [spec args]
                                      :as   crux-del-command}]
                              (->> args
                                   (apply xt.api/q
                                          (xt.api/db ctx)
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
  (let [{:keys [xt/tx-id]
         :as   tx-details} (cond
                             spec
                             , (xt/submit-tx @*db-node
                                             [[:xtdb.tx/fn
                                               ::delete
                                               del-command]])

                             del-command
                             , (xt/submit-tx @*db-node
                                             del-command)

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
    (->> (apply xt/q (xt/db @*db-node) spec args)
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

  (data.model/connect db)

  (data.model/disconnect db)

  (data.model/query db
                    {:spec '{:find  [(pull e [*])]
                             :where [[e :account/id]]}})
  (let [uid "a681c638-7509-4ef3-a816-3ffb42f036a0"]
    (data.model/any
     db
     {:spec '{:find  [(pull a [*])]
              :where [[a :account/user-id user-id]]
              :in    [user-id]}
      :args [uid]}))

  (let [instruments (data.model/query db
                                      {:spec '{:find  [(pull e [*])]
                                               :where [[e :instrument/id]]}})]
    (mapping/command-ents->query-daos fin-instruments/InstrumentDao
                                      instruments))

  (put-delete-fn! {:node                  (connect db)
                   :only-when-not-exists? false})

  (delete! db {:spec '{:find  [?tp]
                       :where [[?tp :trade-pattern/id ?id]]}}
           #_{:spec '{:find  [(pull tp [:trade-pattern/id])]
                      :where [[tp :trade-pattern/id]
                              #_[tp :trade-pattern/name "Breakout"]]}})

  (delete! db [[:xtdb.tx/delete #uuid "c7057fa6-f424-4b47-b1f2-de5ae63fb5fb"]])

  (query db {:spec '{:find  [(pull tp [*])]
                     :where [[tp :trade-pattern/id id]]
                     :in    [id]}
             :args [#uuid "c7057fa6-f424-4b47-b1f2-de5ae63fb5fb"]})

  (query db
         {:spec '{:find  [?tp]
                  :where [[?tp :trade-pattern/id ?id]]}})

  (query db
         {:spec '{:find  [(pull tp [*])]
                  :where [[tp :trade-pattern/id]
                          #_[tp :trade-pattern/name "Breakout"]]}})

  (drop-db! "dev")

  (concat {:a 1 :title "ddd"} {:b 2 :title "qwerrt"})

  (let [entities [{:fin-security/id          #uuid "017b4ed0-c816-b7bc-dc85-2c4f5d5dd7f0"
                   :fin-security/creation-id #uuid "017b4ed4-393f-27d4-24ab-a62973c4098c"
                   :fin-security/amount      6.33
                   :fin-security/ticker      "MO"}
                  {:fin-security/id          #uuid "017b4ed6-7627-debb-7369-b4607e5c77c5"
                   :fin-security/creation-id #uuid "017b4ed4-393f-27d4-24ab-a62973c4098c"
                   :fin-security/amount      33.77
                   :fin-security/ticker      "DM"}
                  {:fin-security/creation-id #uuid "017b4ed4-393f-27d4-24ab-a62973c4098c"
                   :fin-security/amount      17834.88
                   :fin-security/ticker      "BICO"}]]
    (add-all! db entities)
    #_(crux/submit-tx @crux-node (crux-puts entities)))

  (save! db {:trade-pattern/creation-id #uuid "c7057fa6-f424-4b47-b1f2-de5ae63fb5fb",
             :trade-pattern/name        "Breakout",
             :trade-pattern/description "dirt",
             :trade-pattern/user-id     #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f",
             :crux.db/id                #uuid "563ee957-2090-44a0-95ef-db6d57ce0407",
             :trade-pattern/id          #uuid "563ee957-2090-44a0-95ef-db6d57ce0407"})

  (java.util.UUID/randomUUID)

  (query db
         {:spec '{:find  [id cid nm d pid uid]
                  :keys  [trade-pattern/id
                          trade-pattern/creation-id
                          trade-pattern/name
                          trade-pattern/description
                          trade-pattern/parent-id
                          trade-pattern/user-id]
                  :where [[tp :trade-pattern/id id]
                          [tp :trade-pattern/creation-id cid]
                          [tp :trade-pattern/name nm]
                          [tp :trade-pattern/description d]
                          [tp :trade-pattern/user-id uid]
                          [tp :trade-pattern/parent-id pid]]}})

  (query-impl! {:spec     '{:find   [name creation-id]
                            :where  [[tp :trade-pattern/name name]
                                     [tp :trade-pattern/creation-id creation-id]]
                            #_#_:in [name]}
                #_#_:args ["Breakout"]})

  ;; (xt/entity-history
  ;;  (xt/db @crux-node)
  ;;  #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6"
  ;;  :asc
  ;;  {:with-docs? true})

  (let [query-spec '{:find  [fin-sec amount]
                     :where [[fin-sec :fin-security/ticker ticker]
                             [fin-sec :fin-security/amount amount]]
                     :in    [ticker]}
        args       ["DM"]]
    (-> xt/q
        (partial (xt/db @crux-node) query-spec)
        (apply args)))
  )

