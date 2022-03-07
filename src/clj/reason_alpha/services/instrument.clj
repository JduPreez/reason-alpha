(ns reason-alpha.services.instrument
  (:require [malli.core :as m]
            [reason-alpha.model.common :as common]
            [reason-alpha.model.fin-instruments :as fin-instruments]
            [reason-alpha.model.account :as account]
            [taoensso.timbre :as timbre :refer (errorf)]))

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
               account/Account]
              common/getContext]
             (common/result-schema
              [:map
               [:instrument/creation-id uuid?]
               [:instrument/id uuid?]])])

(defn save!
  [fn-repo-save! fn-get-account fn-get-ctx instr]
  (let [{:keys [account/id]}   (fn-get-account instr)
        {:keys [send-message]} (fn-get-ctx instr)]
    (try
      (if id
        (send-message
         [:instrument.command/save!-result
          {:result (-> instr
                       (assoc :instrument/account-id id)
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
