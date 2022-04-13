(ns reason-alpha.services.holding-service
  (:require [malli.core :as m]
            [outpace.config :refer [defconfig]]
            [reason-alpha.integration.eod-api-client :as eod-api-client]
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
        {:keys [send-msg->current-user]} (fn-get-ctx)]
    (try
      (send-msg->current-user
       [:holding.command/save!-result
        {:result (-> instr
                     fn-repo-save!
                     (select-keys [:holding/creation-id
                                   :holding/id]))
         :type   :success}])
      (catch Exception e
        (let [err-msg "Error saving Instrument"]
          (errorf e err-msg)
          (send-msg->current-user
           [:holding.command/save-holding!-result
            {:error       (ex-data e)
             :description (str err-msg ": " (ex-message e))
             :type        :error}]))))))

(defn get-holding [fn-repo-get1 fn-get-ctx {:keys [instrument-id]}]
  (let [{:keys [send-msg->current-user]} (fn-get-ctx)
        instr                  (fn-repo-get1 instrument-id)]
    (send-msg->current-user
     [:holding.query/get-holding-result {:result instr
                                         :type   :success}])))

(defn get-holdings [fn-repo-getn fn-get-account fn-get-ctx _args]
  (let [{acc-id :account/id}   (fn-get-account)
        {:keys [send-msg->current-user]} (fn-get-ctx)
        instrs                 (fn-repo-getn acc-id)]
    (send-msg->current-user
     [:holding.query/get-holdings-result {:result instrs
                                          :type   :success}])))

(defconfig price-quote-interval)

(def *quote-price-users (atom #{}))

(defn- broadcast-prices [fn-repo-get-holdings fn-get-account fn-get-ctx fn-quote-live-prices]
  (let [{:keys [*connected-users
                send-msg->current-user]} (fn-get-ctx)
        quote-interval                   (* 2000 1 #_price-quote-interval)
        broadcast!
        (fn [i]
          (let [uids (:any @*connected-uids)

                ;; First remove all users from *quote-price-users that are not
                ;; in uids, because these users no longer have an active session
                _              (swap! *quote-price-users
                                      (fn [usrs]
                                        (->> usrs
                                             (filter #(some #{%} uids)))))
                qte-price-usrs @*quote-price-users]
            (doseq [acc-id qte-price-usrs
                    :let   [api-token           (-> acc-id
                                                    fn-get-account
                                                    :account/subscriptions
                                                    :subscription/eod-historical-data
                                                    :api-token)
                            tickers             (->> acc-id
                                                     fn-repo-get-holdings
                                                     (map (fn [{:keys [holding-id eod-historical-data]}]
                                                            [holding-id eod-historical-data])))
                            price-results       (fn-quote-live-prices api-token tickers {:batch-size 2})
                            fn-send-price-quote #(send-msg acc-id [:price/quote %])]]
              (utils/do-if-realized price-results fn-send-price-quote))))]

    (go-loop [i 0]
      (<! (async/timeout quote-interval))
      (broadcast! i)
      (recur (inc i)))))

(comment
  (utils/new-uuid)

  (let [uids     #{#uuid "d1f5984c-c2a0-475a-8df2-5e7ef04dc989" ;; Hasn't requested prices
                   #uuid "d3cfc2f6-d38c-4e5e-b4b1-7f906315d8e0" ;; Hasn't requested prices
                   #uuid "bf0bb6db-7589-4e4d-aa82-a84608263dab"
                   #uuid "6a23dee4-8a0f-4416-bb7a-3fa5e8b22590"}
        qp-users #{#uuid "22e0743b-bf4e-4982-9a1d-d3bc97c08372" ;; Disconnected
                   #uuid "ec0760d6-fe13-4336-b978-6df86ab4a43b" ;; Disconnected
                   #uuid "bf0bb6db-7589-4e4d-aa82-a84608263dab"
                   #uuid "6a23dee4-8a0f-4416-bb7a-3fa5e8b22590"}]
    
    ;; (reset! *quote-price-users qp-users)
    ;; (swap! *quote-price-users
    ;;        (fn [usrs]
    ;;          (->> usrs
    ;;               (filter #(some #{%} uids)))))
    ;; @*quote-price-users

    )

  )

(defn get-positions
  [{:keys [fn-repo-getn fn-get-ctx response-msg-event]}]
  (fn [_]
    (let [{send-msg             :send-msg->current-user
           {acc-id :account/id} :user-account} (fn-get-ctx)
          ents                                 (fn-repo-getn acc-id)]
      (clojure.pprint/pprint {::getn-msg-fn [response-msg-event ents]})
      (send-msg
       [response-msg-event {:result ents
                            :type   :success}]))))

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
