(ns reason-alpha.services.trade-pattern
  (:require  [clojure.pprint :as pprint]
             [reason-alpha.data.repositories.trade-pattern :as repo.trade-pattern]
             [clojure.pprint :as pprint]))

; TODO: Improve error handling
(defn getn [_qry] #_[{{{user-id :user-id} :path} :parameters}]
  (pprint/pprint {::get-trade-patterns _qry})
  {:result (repo.trade-pattern/getn)}) ;; (choose db [:trade-pattern/*])

(defn get1 [id]
  {:result (repo.trade-pattern/get1 id)})

(defn save! [ent]
  {:result (repo.trade-pattern/save! ent)})

(defn delete! [ids]
  {:result (repo.trade-pattern/delete! ids)})

#_(defmethod server/event-msg-handler
  :trade-pattern.query/get
  [{:keys [?reply-fn] :as event}]
  (pprint/pprint {:trade-pattern.query/get event})
  (when ?reply-fn
    (?reply-fn (get-trade-patterns))))
