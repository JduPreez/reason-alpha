(ns reason-alpha.services.position
  (:require [malli.core :as m]
            [reason-alpha.model.common :as common]
            [reason-alpha.model.portfolio-management :as portfolio-management]
            [taoensso.timbre :as timbre :refer (errorf)]))

(m/=> save! [:=>
             [:cat
              [:=>
               [:cat
                :any
                portfolio-management/Position]
               portfolio-management/Position]]
             (common/result-schema portfolio-management/Position)])

(defn save!
  [fn-repo-save! fn-get-account ent]
  (try
    (let [{:keys [account/id]} (fn-get-account)]

      (if id
        {:result (fn-repo-save! (assoc ent :position/account-id id))
         :type   :success}
        {:description "No account found."
         :type        :error}))

    (catch Exception e
      (errorf e "Error saving Position")
      {:error (ex-data e)
       :type  :error})))
