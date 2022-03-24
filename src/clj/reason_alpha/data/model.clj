(ns reason-alpha.data.model
  (:require [reason-alpha.model.core :as model]))

(def =>delete! [:=>
                [:catn [:vector any?]]
                [:map
                 [:was-deleted? boolean?]
                 [:num-deleted int?]
                 [:message {:optional true} string?]]])

(defprotocol DataBase
  (connect [_] _)
  (disconnect [_] _)
  (query [_ _query-spec] _)
  (any [_ _query-spec] _)
  (delete! [_ _query-spec] _)
  (save! [_ _entity & [options]] _)
  (add-all! [_ _entities] _))
