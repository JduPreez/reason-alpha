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
  [fn-repo-save! fn-get-account fn-get-ctx {acc-id :instrument/account-id
                                            :as    instrument}]
  (let [instr                  (if acc-id
                                 instrument
                                 (->> (fn-get-account)
                                      :account/id
                                      (assoc instrument :instrument/account-id)))
        {:keys [send-message]} (fn-get-ctx)]
    (try
      (send-message
       [:instrument.command/save!-result
        {:result (-> instr
                     fn-repo-save!
                     (select-keys [:instrument/creation-id
                                   :instrument/id]))
         :type   :success}])
      (catch Exception e
        (let [err-msg "Error saving Instrument"]
          (errorf e err-msg)
          (send-message
           [:instrument.command/save!-result
            {:error       (ex-data e)
             :description (str err-msg ": " (ex-message e))
             :type        :error}]))))))

(defn get1 [fn-repo-get1 fn-get-ctx {:keys [instrument-id]}]
  (let [{:keys [send-message]} (fn-get-ctx)
        instr                  (fn-repo-get1 instrument-id)]
    (send-message
     [:instrument.query/get1-result {:result instr
                                     :type   :success}])))

(defn getn [fn-repo-getn fn-get-account fn-get-ctx _args]
  (let [{acc-id :account/id}   (fn-get-account)
        {:keys [send-message]} (fn-get-ctx)
        instrs                 (fn-repo-getn acc-id)]
    (send-message
     [:instrument.query/getn-result {:result instrs
                                     :type   :success}])))
