(ns reason-alpha.services.trade-pattern
  (:require  [clojure.pprint :as pprint]))

; TODO: Improve error handling
(defn getn [fn-repo-getn _qry]
  {:result (fn-repo-getn)
   :type   :success})

(defn get1 [fn-repo-get1 id]
  {:result (fn-repo-get1 id)
   :type   :success})

(defn save! [fn-repo-save! ent]
  {:result (fn-repo-save! ent)
   :type   :success})

(defn delete! [fn-repo-delete! ids]
  {:result (fn-repo-delete! ids)
   :type   :success})
