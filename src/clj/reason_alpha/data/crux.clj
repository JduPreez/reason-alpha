(ns reason-alpha.data.crux
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
            [crux.api :as crux]
            [me.raynes.fs :as fs]
            [mount.lite :refer (defstate) :as mount]
            [outpace.config :refer [defconfig]]
            [reason-alpha.data :refer [DataBase save! add-all!]]
            [reason-alpha.model :as model])
  (:gen-class))

(defconfig data-dir) ;; "data"
(defconfig db-name) ;; "dev"

(defn drop-db! [db]
  (fs/delete-dir (str data-dir "/" db)))

(defn- put-delete-fn!
  [{node                  :node
    only-when-not-exists? :only-when-not-exists?
    :or                   {only-when-not-exists? true}}]
  (let [already-exists? (-> (crux/db node)
                            (crux/q  '{:find  [(count fun)]
                                       :where [[del-tx-fn :crux.db/fn fun]
                                               [del-tx-fn :crux.db/id ::delete]]})
                            first
                            first
                            (= 0))]
    (when (or (and only-when-not-exists?
                   (false? already-exists?))
              (false? only-when-not-exists?))
        (crux/submit-tx node
                        [[:crux.tx/put
                          {:crux.db/id ::delete
                           :crux.db/fn
                           , '(fn [ctx {:keys [spec args]
                                        :as   crux-del-command}]
                                (->> args
                                     (apply crux.api/q
                                            (crux.api/db ctx)
                                            spec)
                                     (map #(let [doc (first %)
                                                 id  (if (map? doc)
                                                       (-> % first vals first)
                                                       doc)]
                                             [:crux.tx/delete id]))
                                     vec))}]]))))

(defn start-crux! []
  (clojure.pprint/pprint ::start-crux!)
  (let [fn-kv-store (fn [dir]
                      {:kv-store {:crux/module 'crux.rocksdb/->kv-store
                                  :db-dir      (-> data-dir
                                                   (str "/" db-name "/" dir)
                                                   io/file)
                                  :sync?       true}})
        node        (crux/start-node
                     {:crux/tx-log         (fn-kv-store "tx-log")
                      :crux/document-store (fn-kv-store "doc-store")
                      :crux/index-store    (fn-kv-store "index-store")})]
    (put-delete-fn! {:node node})
    node))

(defstate crux-node
  :start (start-crux!)
  :stop ,(.close @crux-node))

(defn- maybe-add-id [entities]
  (->> entities
       (map #(let [id-key (model/entity-id-key %)]
               (-> %
                   id-key
                   (or (UuidCreator/getLexicalOrderGuid))
                   (as-> idv (assoc % :crux.db/id idv))
                   (as-> ent (assoc ent id-key (:crux.db/id ent))))))))

(defn- crux-puts [entities]
  (->> entities
       (map (fn [ent] [:crux.tx/put ent]))
       vec))

(defn save-impl! [entities]
  (let [ents-with-ids (maybe-add-id entities)]
    (crux/submit-tx @crux-node (crux-puts ents-with-ids))
    ents-with-ids))

(defn delete-impl! [{:keys [spec] :as crux-del-command}]
  (let [{:keys [crux.tx/tx-id]
         :as   tx-details} (cond
                             spec
                             , (crux/submit-tx @crux-node
                                               [[:crux.tx/fn
                                                 ::delete
                                                 crux-del-command]])

                             crux-del-command
                             , (crux/submit-tx @crux-node
                                               crux-del-command)

                             :else {:was-deleted? false})]
    {:tx-details   tx-details
     :was-deleted? (not (nil? tx-id))}))

(def db (let [add-al-impl! save-impl!
              sav-impl!    save-impl!
              del-impl!    delete-impl!]
          (reify DataBase
            (connect [_]
              (mount/start #'crux-node)
              @crux-node)

            (disconnect [_] (mount/stop #'crux-node))

            (query [_ {:keys [spec args]}]
              (->> (apply crux/q (crux/db @crux-node) spec args)
                   (map (fn [[entity :as all]]
                          (if (map? entity)
                            entity
                            all)))))

            (any [this query-spec]
              (first (.query this query-spec)))

            ;; Delete command's spec should only return :crux.db/id
            (delete! [this delete-command]
              (del-impl! delete-command))

            (save! [this entity]
              (save! this entity sav-impl!))

            (save! [_ entity fn-save-impl!]
              (first (fn-save-impl! [entity])))

            (add-all! [this entities]
              (add-all! this entities add-al-impl!))

            (add-all! [_ entities fn-add-all-impl!]
              (fn-add-all-impl! entities)))))

(comment

  (require '[reason-alpha.data :refer [add-all! query save! connect delete!]])

  (connect db)

  (put-delete-fn! {:node                  (connect db)
                   :only-when-not-exists? false})

  (delete! db {:spec '{:find  [?tp]
                       :where [[?tp :trade-pattern/id ?id]]}}
           #_{:spec '{:find  [(pull tp [:trade-pattern/id])]
                      :where [[tp :trade-pattern/id]
                              #_[tp :trade-pattern/name "Breakout"]]}})

  

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

  

  (query db {:spec '{:find  [(pull tp [*])]
                     :where [[tp :trade-pattern/id id]]
                     :in    [id]}
             :args [#uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6"]})

  (query-impl! {:spec     '{:find   [name creation-id]
                            :where  [[tp :trade-pattern/name name]
                                     [tp :trade-pattern/creation-id creation-id]]
                            #_#_:in [name]}
                #_#_:args ["Breakout"]})

  

  (let [ids '(#uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6"
              #uuid "017d3e1a-348d-d3f3-1bf8-03388d9db527")]
    (-> @crux-node
        crux/db
        (crux/q  '{:find  [tp]
                   :where [[tp :trade-pattern/id id]]
                   :in    [[id ...]]}
                 ids)))

  (let [query-spec '{:find  [fin-sec amount]
                     :where [[fin-sec :fin-security/ticker ticker]
                             [fin-sec :fin-security/amount amount]]
                     :in    [ticker]}
        args       ["DM"]]
    (-> crux/q
        (partial (crux/db @crux-node) query-spec)
        (apply args)))
  )

