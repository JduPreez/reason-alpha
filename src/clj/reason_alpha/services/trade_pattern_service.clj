(ns reason-alpha.services.trade-pattern-service
  (:require [malli.core :as m]
            [reason-alpha.model.accounts :as accounts]
            [reason-alpha.model.common :as common]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.model.portfolio-management :as portfolio-management]
            [taoensso.timbre :as timbre :refer (errorf)]))

(defn getn [fn-repo-getn fn-get-account]
  (let [{acc-id :account/id} (fn-get-account)
        tpatterns            (fn-repo-getn acc-id)]
    {:result tpatterns
     :type   :success}))

(defn get1 [fn-repo-get1 {:keys [trade-pattern-id]}]
  (let [tpattern (fn-repo-get1 trade-pattern-id)]
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
  [fn-repo-save! fn-get-account {acc-id :trade-pattern/account-id
                                 :as    trade-pattern}]
  (let [tpattern (if acc-id
                   trade-pattern
                   (->> (fn-get-account)
                        :account/id
                        (assoc trade-pattern :trade-pattern/account-id)))]
    (try
      {:result (->> tpattern
                    fn-repo-save!
                    (conj [])
                    (mapping/command-ent->query-dto
                     portfolio-management/TradePatternDto))
       :type   :success}
      (catch Exception e
        (let [err-msg "Error saving Trade Pattern"]
          (errorf e err-msg)
          {:error       (ex-data e)
           :description (str err-msg ": " (ex-message e))
           :type        :error})))))
