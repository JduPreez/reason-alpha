(ns reason-alpha.services.trade-pattern
  (:require [malli.core :as m]
            [reason-alpha.model.accounts :as accounts]
            [reason-alpha.model.common :as common]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.model.portfolio-management :as portfolio-management]
            [taoensso.timbre :as timbre :refer (errorf)]))

(defn getn [fn-repo-getn fn-get-account args]
  (let [{acc-id :account/id} (fn-get-account)
        tpatterns            (fn-repo-getn acc-id)]
    {:result tpatterns
     :type   :success}))

(defn get1 [fn-repo-get1 fn-get-account {:keys [trade-pattern-id] :as args}]
  (let [{acc-id :account/id} (fn-get-account)
        tpattern             (fn-repo-get1 acc-id trade-pattern-id)]
    {:result tpattern
     :type   :success}))

(m/=> save! [:=>
             [:cat
              [:=>
               [:cat
                :any
                portfolio-management/TradePattern]
               portfolio-management/TradePattern]
              [:=>
               [:cat
                :any]
               accounts/Account]
              portfolio-management/TradePattern]
             (common/result-schema
              portfolio-management/TradePatternDto)])

(defn save!
  [fn-repo-save! fn-get-account tpattern]
  (let [{account-id :account/id} (fn-get-account)]
    (try
      (if account-id
        {:result (-> tpattern
                     (assoc :trade-pattern/account-id account-id)
                     fn-repo-save!
                     (as-> tp (mapping/command-ent->query-dto
                               portfolio-management/TradePatternDto tp)))
         :type   :success}
        {:description "No account found."
         :type        :error})

      (catch Exception e
        (let [err-msg "Error saving Trade Pattern"]
          (errorf e err-msg)
          {:error       (ex-data e)
           :description (str err-msg ": " (ex-message e))
           :type        :error})))))

#_(defn delete! [fn-repo-delete! fn-get-account ids]
  (let [{acc-id :account/id} (fn-get-account)]
    (try
      (if acc-id
        {:result (fn-repo-delete! acc-id ids)
         :type   :success}
        {:description "No account found."
         :type        :error})
      (catch Exception e
        (let [err-msg "Error deleting Trade Pattern"]
          (errorf e err-msg)
          {:error       (ex-data e)
           :description (str err-msg ": " (ex-message e))
           :type        :error})))))
