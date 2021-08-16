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
  (:require [clojure.java.io :as io]
            [crux.api :as crux]
            [reason-alpha.data :refer [DataBase]]))

(defn start-crux! []
  (letfn [(kv-store [dir]
            {:kv-store {:crux/module 'crux.rocksdb/->kv-store
                        :db-dir      (io/file dir)
                        :sync?       true}})]
    (crux/start-node
     {:crux/tx-log         (kv-store "data/dev/tx-log")
      :crux/document-store (kv-store "data/dev/doc-store")
      :crux/index-store    (kv-store "data/dev/index-store")})))

(def crux-node (delay (start-crux!)))

(def db (let [_ 0]
          (reify DataBase
            (connect [_] @crux-node)

            (disconnect [_] (.close @crux-node))

            (select [_ query-spec]
              nil)

            (any [this query-spec]
              nil)

            (delete! [this query-spec]
              {:was-deleted? nil
               :num-deleted  nil})

            (save! [this rentity]
              nil)

            (save! [_ rentity save-impl!-fn]
              nil)

            (add-all! [this entities]
              nil)

            (add-all! [_ entities add-all-impl!-fn]
              (let [crux-ents (map #(
                                     )
                                   entities)])))))
