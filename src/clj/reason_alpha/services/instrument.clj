(ns reason-alpha.services.instrument
  (:require [malli.core :as m]
            [reason-alpha.model.common :as common]
            [reason-alpha.model.fin-instruments :as fin-instruments]
            [taoensso.timbre :as timbre :refer (errorf)]))

(m/=> save! [:=>
             [:cat
              [:=>
               [:cat
                :any
                fin-instruments/Instrument]
               fin-instruments/Instrument]]
             (common/result-schema fin-instruments/Instrument)])

(defn save!
  [fn-repo-save! fn-get-account ent]
  (try
    (let [{:keys [account/id]} (fn-get-account ent)]

      (if id
        {:result (fn-repo-save! (assoc ent :instrument/account-id id))
         :type   :success}
        {:description "No account found."
         :type        :error}))

    (catch Exception e
      (let [err-msg "Error saving Position"]
        (errorf e err-msg)
        {:error       (ex-data e)
         :description (str err-msg ": " (ex-message e))
         :type        :error}))))
