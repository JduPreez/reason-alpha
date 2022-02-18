(ns reason-alpha.services.trade-pattern
  (:require  [clojure.pprint :as pprint]))

; TODO: Improve error handling
(defn getn [fn-repo-getn _qry]
  (pprint/pprint {::get-trade-patterns _qry})
  {:result (fn-repo-getn)}) ;; (choose db [:trade-pattern/*])

(defn get1 [fn-repo-get1 id]
  {:result (fn-repo-get1 id)})

(defn save! [fn-repo-save! ent]
  {:result (fn-repo-save! ent)})

(defn delete! [fn-repo-delete! ids]
  {:result (fn-repo-delete! ids)})
