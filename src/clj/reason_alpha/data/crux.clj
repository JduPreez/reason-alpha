(ns reason-alpha.data.crux
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
              nil))))
