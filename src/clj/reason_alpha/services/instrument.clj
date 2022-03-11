(ns reason-alpha.services.instrument
  (:require [malli.core :as m]
            [reason-alpha.model.common :as common]
            [reason-alpha.model.fin-instruments :as fin-instruments]
            [reason-alpha.model.accounts :as accounts]
            [taoensso.timbre :as timbre :refer (errorf)]
            [reason-alpha.utils :as utils]))

(m/=> save! [:=>
             [:cat
              [:=>
               [:cat
                :any
                fin-instruments/Instrument]
               fin-instruments/Instrument]
              [:=>
               [:cat
                :any]
               accounts/Account]
              common/getContext]
             (common/result-schema
              [:map
               [:instrument/creation-id uuid?]
               [:instrument/id uuid?]])])

(defn save!
  [fn-repo-save! fn-get-account fn-get-ctx instr]
  (clojure.pprint/pprint {::save!-1 {:FRS fn-repo-save!
                                     :FGA fn-get-account
                                     :FGC fn-get-ctx
                                     :I   instr}})
  (let [{account-id :account/id} (fn-get-account instr)
        {:keys [send-message]}   (fn-get-ctx instr)]
    (clojure.pprint/pprint {::save!-2 account-id})
    (try
      (if account-id
        (send-message
         [:instrument.command/save!-result
          {:result (-> instr
                       (assoc :instrument/account-id account-id)
                       fn-repo-save!
                       (select-keys [:instrument/creation-id
                                     :instrument/id]))
           :type   :success}])
        (send-message
         [:instrument.command/save!-result
          {:description "No account found."
           :type        :error}]))

      (catch Exception e
        (let [err-msg "Error saving Position"]
          (errorf e err-msg)
          (send-message
           [:instrument.command/save!-result
            {:error       (ex-data e)
             :description (str err-msg ": " (ex-message e))
             :type        :error}]))))))

(defn get1 [fn-repo-get1 fn-get-account fn-get-ctx {:keys [instrument-id] :as args}]
  (let [{:keys [account/user-id]} (fn-get-account args)
        {:keys [send-message]}    (fn-get-ctx args)
        instr                     (fn-repo-get1 user-id instrument-id)]
    (send-message
     [:model.query/getn-result {:result instr
                                :type   :success}])))
