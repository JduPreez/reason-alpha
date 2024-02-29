(ns reason-alpha.data.temp-mem-db
  (:require [reason-alpha.data.model :as data.model :refer [DataBase]]))

(deftype TempMemDb [*db]
  DataBase
  (disconnect
    [_]
    (reset! *db nil))

  (connect
    [_]
    (reset! *db [])
    @*db)

  (query [this {:keys [spec args account-id-key role] :as qry}]
    nil)

  (any [this query-spec]
    nil)

  (delete! [this delete-cmd]
    nil)

  (save! [this entity {:keys [role]}]
    (conj @*db entity))

  (save! [this entity]
    (.save! this entity nil))

  (save-all! [this entities {:keys [role]}]
    (concat @*db entities))

  (save-all! [this entities]
    (.save-all! this entities nil)))
