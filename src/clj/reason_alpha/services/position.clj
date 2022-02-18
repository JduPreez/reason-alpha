(ns reason-alpha.services.position
  (:require [reason-alpha.model.common :as common]
            [reason-alpha.model.portfolio-management :as portfolio-management]
            [taoensso.timbre :as timbre :refer (errorf)]))

(defn save!
  {:malli/schema
   [:=>
    [:cat
     [:=>
      [:cat
       :any
       portfolio-management/Position]
      portfolio-management/Position]]
    common/Result]}
  [fn-repo-save! ent]
  (try

    {:result (fn-repo-save! ent)
     :type   :success}

    (catch Exception e
      (errorf e "Error saving Position")
      {:result (ex-data e)
       :type   :error})))
