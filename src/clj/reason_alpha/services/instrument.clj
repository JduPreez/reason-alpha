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
              common/getContext
              fin-instruments/Instrument]
             (common/result-schema
              [:map
               [:instrument/creation-id uuid?]
               [:instrument/id uuid?]])])

(defn save!
  [fn-repo-save! fn-get-account fn-get-ctx instr]
  (let [{account-id :account/id} (fn-get-account instr)
        {:keys [send-message]}   (fn-get-ctx instr)]
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
        (let [err-msg "Error saving Instrument"]
          (errorf e err-msg)
          (send-message
           [:instrument.command/save!-result
            {:error       (ex-data e)
             :description (str err-msg ": " (ex-message e))
             :type        :error}]))))))

(defn get1 [fn-repo-get1 fn-get-account fn-get-ctx {:keys [instrument-id] :as args}]
  (let [{acc-id :account/id}   (fn-get-account args)
        {:keys [send-message]} (fn-get-ctx args)
        instr                  (fn-repo-get1 acc-id instrument-id)]
    (send-message
     [:instrument.query/get1-result {:result instr
                                     :type   :success}])))

(defn getn [fn-repo-getn fn-get-account fn-get-ctx args]
  (let [{acc-id :account/id}   (fn-get-account args)
        {:keys [send-message]} (fn-get-ctx args)
        instrs                 (fn-repo-getn acc-id)]
    (send-message
     [:instrument.query/getn-result {:result instrs
                                     :type   :success}])))
