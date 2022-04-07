(ns reason-alpha.services.holding-service
  (:require [malli.core :as m]
            [reason-alpha.model.accounts :as accounts]
            [reason-alpha.model.common :as common]
            [reason-alpha.model.fin-instruments :as fin-instruments]
            [reason-alpha.model.portfolio-management :as portfolio-management]
            [reason-alpha.utils :as utils]
            [taoensso.timbre :as timbre :refer (errorf)]))

(m/=> save-holding! [:=>
                     [:cat
                      [:=>
                       [:cat
                        :any
                        portfolio-management/Holding]
                       portfolio-management/Holding]
                      [:=>
                       [:cat
                        :any]
                       accounts/Account]
                      common/getContext
                      portfolio-management/Holding]
                     (common/result-schema
                      [:map
                       [:holding/creation-id uuid?]
                       [:holding/id uuid?]])])

(defn save-holding!
  [fn-repo-save! fn-get-account fn-get-ctx {acc-id :holding/account-id
                                            :as    instrument}]
  (let [instr                  (if acc-id
                                 instrument
                                 (->> (fn-get-account)
                                      :account/id
                                      (assoc instrument :holding/account-id)))
        {:keys [send-message]} (fn-get-ctx)]
    (try
      (send-message
       [:holding.command/save!-result
        {:result (-> instr
                     fn-repo-save!
                     (select-keys [:holding/creation-id
                                   :holding/id]))
         :type   :success}])
      (catch Exception e
        (let [err-msg "Error saving Instrument"]
          (errorf e err-msg)
          (send-message
           [:holding.command/save-holding!-result
            {:error       (ex-data e)
             :description (str err-msg ": " (ex-message e))
             :type        :error}]))))))

(defn get-holding [fn-repo-get1 fn-get-ctx {:keys [instrument-id]}]
  (let [{:keys [send-message]} (fn-get-ctx)
        instr                  (fn-repo-get1 instrument-id)]
    (send-message
     [:holding.query/get-holding-result {:result instr
                                         :type   :success}])))

(defn get-holdings [fn-repo-getn fn-get-account fn-get-ctx _args]
  (let [{acc-id :account/id}   (fn-get-account)
        {:keys [send-message]} (fn-get-ctx)
        instrs                 (fn-repo-getn acc-id)]
    (send-message
     [:holding.query/get-holdings-result {:result instrs
                                          :type   :success}])))

#_(m/=> save! [:=>
             [:cat
              [:=>
               [:cat
                :any
                portfolio-management/Position]
               portfolio-management/Position]]
             (common/result-schema portfolio-management/Position)])

#_(defn save-position!
  [fn-repo-save-position! fn-get-account ent]
  (try
    (let [{:keys [account/id]} (fn-get-account)]

      (if id
        {:result (fn-repo-save-position! (assoc ent :position/account-id id))
         :type   :success}
        {:description "No account found."
         :type        :error}))

    (catch Exception e
      (errorf e "Error saving Position")
      {:error (ex-data e)
       :type  :error})))
