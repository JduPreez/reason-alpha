(ns reason-alpha.services.holding-service
  (:require [clojure.core.async :as async  :refer (<! go-loop)]
            [malli.core :as m]
            [outpace.config :refer [defconfig]]
            [reason-alpha.integration.eod-api-client :as eod-api-client]
            [reason-alpha.model.core :as model]
            [reason-alpha.model.accounts :as accounts]
            [reason-alpha.model.common :as common]
            [reason-alpha.model.fin-instruments :as fin-instruments]
            [reason-alpha.model.portfolio-management :as portfolio-management]
            [reason-alpha.utils :as utils]
            [taoensso.timbre :as timbre :refer (errorf)]
            [traversy.lens :as lens]
            [reason-alpha.integration.fake-eod-api-client :as eod]))

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
  (let [holding                  (if acc-id
                                 instrument
                                 (->> (fn-get-account)
                                      :account/id
                                      (assoc instrument :holding/account-id)))
        {:keys [send-message]} (fn-get-ctx)]
    (try
      (send-message
       [:holding.command/save-holding!-result
        {:result (-> holding
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

(defn get-holding [fn-repo-get1 fn-get-ctx id]
  (let [{:keys [send-message]} (fn-get-ctx)
        holding                (fn-repo-get1 id)]
    (send-message
     [:holding.query/get-holding-result {:result holding
                                         :type   :success}])))

(defn get-holdings [fn-repo-get-positions fn-get-account fn-get-ctx]
  (let [{acc-id :account/id}   (fn-get-account)
        {:keys [send-message]} (fn-get-ctx)
        holdings               (fn-repo-get-positions
                                {:account-id acc-id})]
    (send-message
     [:holding.query/get-holdings-result {:result holdings
                                          :type   :success}])))

(defn- aggregate-holding-positions [positions]
  (let [holding-pos    (-> positions
                           (lens/view
                            (lens/only
                             #(nil?
                               (:holding-position-id %))))
                           first)
        sub-positions  (-> positions
                           (lens/view
                            (lens/only
                             :holding-position-id)))
        holding-pos    (when holding-pos
                         (portfolio-management/assoc-aggregate-fields
                          holding-pos sub-positions))
        sub-positions  (when (seq sub-positions)
                         (map portfolio-management/assoc-aggregate-fields
                              sub-positions))
        pos-with-comps (cond-> []
                         (seq sub-positions) (into sub-positions)
                         holding-pos         (conj holding-pos)
                         :always             (common/compute
                                              {:computations
                                               portfolio-management/position-dto-functions}))]
    pos-with-comps))

(defn- assoc-close-prices-fn [fn-repo-get-acc-by-uid fn-quote-live-prices & [{:keys [batch-size]}]]
  (fn [account-id positions]
    (let [api-token         (-> account-id
                                fn-repo-get-acc-by-uid
                                :account/subscriptions
                                :subscription/eod-historical-data
                                :api-token)
          tickers           (->> positions
                                 (map (fn [{:keys [holding-id eod-historical-data]}]
                                        (when (and holding-id eod-historical-data)
                                          [holding-id eod-historical-data])))
                                 (remove nil?)
                                 distinct)
          prices            (fn-quote-live-prices api-token tickers {:batch-size batch-size})
          pos-with-close-pr (->> prices
                                 (pmap #(deref %))
                                 (mapcat identity)
                                 (mapcat (fn [{price-hid :holding-id
                                               price     :price-close}]
                                           (->> positions
                                                (filter (fn [{:keys [holding-id]}]
                                                          (= holding-id price-hid)))
                                                (mapv (fn [{:keys [status] :as p}]
                                                        (if (#{:open} status)
                                                          (assoc p :close-price price)
                                                          p)))))))]
      pos-with-close-pr)))

(comment

  (require '[reason-alpha.integration.fake-eod-api-client :as eod])
  (let [fun (assoc-close-prices-fn (fn [aid]
                                     {:account/subscriptions
                                      {:subscription/eod-historical-data
                                       {:api-token "djhjdhd"}}})
                                   eod/quote-live-prices)
        p   [{:holding
              [#uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5"
               "Sony"],
              :open-price 33,
              :open-time
              #inst "2022-05-18T00:00:00.000-00:00",
              :stop       20,
              :position-creation-id
              #uuid "ab5e3c79-3334-4e52-86e5-e1eec94eaccc",
              :status     :open,
              :position-id
              #uuid "01811a3f-638f-d0de-4ddd-ec224bc81cf1",
              :holding-position-id
              #uuid "0180098b-e65e-d7ce-645b-41eef737fa0c",
              :holding-id
              #uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5",
              :quantity   23,
              :eod-historical-data
              "6758.TSE",
              :long-short [:long ""]}
             {:trade-pattern
              [#uuid "01800865-9069-63c7-9c6c-4d24cdcefc9a"
               "Breakout"],
              :holding
              [#uuid "018004bb-227c-d6de-69ac-bc1eab688ab5"
               "Walt Disney"],
              :open-price  23,
              :open-time
              #inst "2022-04-06T00:00:00.000-00:00",
              :stop        56,
              :position-creation-id
              #uuid "74e921b2-fe79-454d-b47c-08c43b298019",
              :status      :closed,
              :close-price 95.86,
              :position-id
              #uuid "0180098b-e65e-d7ce-645b-41eef737fa0c",
              :holding-id
              #uuid "018004bb-227c-d6de-69ac-bc1eab688ab5",
              :quantity    676,
              :eod-historical-data
              "DIS.US",
              :long-short  [:long ""]}
             {:holding
              [#uuid "01809f38-c167-6811-e9ef-c2edd166236d"
               "Unity"],
              :open-price          34,
              :open-time
              #inst "2022-05-05T00:00:00.000-00:00",
              :stop                34,
              :position-creation-id
              #uuid "9f91f95c-43c3-46e6-a661-5409959a42b2",
              :status              :closed,
              :close-price         46.72,
              :position-id
              #uuid "0180ae8d-abff-be83-dcde-a31bfe42ab41",
              :holding-position-id
              #uuid "0180098b-e65e-d7ce-645b-41eef737fa0c",
              :holding-id
              #uuid "01809f38-c167-6811-e9ef-c2edd166236d",
              :quantity            33,
              :eod-historical-data "U.US",
              :long-short          [:long ""]}
             {:trade-pattern
              [#uuid "0180088d-aa18-6709-de16-4d2e56126947"
               "zzzzzz"],
              :holding
              [#uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5"
               "Sony"],
              :open-price  23,
              :open-time
              #inst "2022-04-20T00:00:00.000-00:00",
              :stop        2342,
              :position-creation-id
              #uuid "0d4a7fdf-5ab0-4d08-a35a-c5b23fa46c6e",
              :status      :closed,
              :close-price 8.07,
              :position-id
              #uuid "018008e1-a0db-2638-b2e2-4e9f0e332d11",
              :holding-position-id
              #uuid "0180098b-e65e-d7ce-645b-41eef737fa0c",
              :holding-id
              #uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5",
              :quantity    50,
              :eod-historical-data
              "6758.TSE",
              :long-short  [:long ""]}
             ]
        ]
    #_(-> (fun "test" p) first deref)
    (fun "test" p))

  )

(defn get-holding-positions-fn
  [fn-repo-get-holding-positions fn-repo-get-acc-by-uid fn-quote-live-prices fn-get-ctx]
  (let [fn-assoc-close-prices (assoc-close-prices-fn fn-repo-get-acc-by-uid
                                                     fn-quote-live-prices)]
    (fn [{:keys [position/id position/holding-position-id]}]
      (let [pos-id                               (or holding-position-id id)
            {send-message         :send-message
             {acc-id :account/id} :user-account} (fn-get-ctx)
            result                               (->> pos-id
                                                      fn-repo-get-holding-positions
                                                      (fn-assoc-close-prices acc-id)
                                                      aggregate-holding-positions)]
        (send-message [:holding.query/get-holding-positions-result
                       result])))))

(def *broadcast-holdings-positions (atom #{}))

(defconfig price-quote-interval)

(defn get-holdings-positions
  [fn-repo-get-holdings-positions fn-repo-get-acc-by-uid fn-quote-live-prices
   broadcast? {:keys [fn-get-ctx account-id send-message]}]
  (let [{send-msg :send-message
         :as      ctx}        (when fn-get-ctx
                                (fn-get-ctx))
        acc-id                (or account-id
                                  (and ctx
                                       (get-in ctx [:user-account :account/id])))
        send-msg              (or send-msg
                                  #(send-message acc-id %))
        fn-assoc-close-prices (assoc-close-prices-fn fn-repo-get-acc-by-uid
                                                     fn-quote-live-prices)
        gpositions            (->> {:account-id acc-id
                                    :role       (if account-id
                                                  :system
                                                  :member)}
                                   fn-repo-get-holdings-positions
                                   (group-by (fn [{:keys [position-id holding-position-id]}]
                                               (or holding-position-id
                                                   position-id))))]

    (when broadcast? (swap! *broadcast-holdings-positions conj acc-id))

    (doseq [[_gpos-id posns] gpositions]
      (future
        (try
          (let [result (->> posns
                            (fn-assoc-close-prices acc-id)
                            aggregate-holding-positions)]
            (send-msg
             [:holding.query/get-holdings-positions-result result]))
          (catch Exception e
            (let [err-msg "Error getting holdings positions"]
              (errorf e err-msg)
              (send-message
               [:holding.query/get-holdings-positions-result
                {:error       (ex-data e)
                 :description (str err-msg ": " (ex-message e))
                 :type        :error}]))))))))

(defn broadcast-holdings-positions
  [fn-get-holdings-positions {:keys [send-message *connected-users]}]
  (let [quote-interval (* 60000 price-quote-interval)
        broadcast!
        (fn [i]
          (let [uids (:any @*connected-users)
                ;; First remove all users from *quote-price-users that are not
                ;; in uids, because these users no longer have an active session
                _    (swap! *broadcast-holdings-positions
                            (fn [usrs]
                              (->> usrs
                                   (filter #(some #{%} uids))
                                   set)))]

            (clojure.pprint/pprint {::broadcast-holdings-positions @*broadcast-holdings-positions})

            (doseq [acc-id @*broadcast-holdings-positions]
              (println (str i ") Broadcast holdings positions " acc-id))
              (fn-get-holdings-positions {:send-message send-message
                                          :account-id   acc-id}))))]

    (go-loop [i 0]
      (println "Broadcast holdings positions " i)
      (<! (async/timeout quote-interval))
      (broadcast! i)
      (recur (inc i)))))

(defn save-position!
  [fn-repo-save! fn-get-account fn-get-ctx
   {acc-id          :position/account-id
    {quantity :trade-transaction/quantity
     :as      open} :position/open
    close           :position/close
    hid             :position/holding-id
    status          :position/status
    :as             position}]
  (let [pos                    (cond-> position
                                 (nil? acc-id) (assoc :position/account-id (-> (fn-get-account)
                                                                               :account/id))
                                 open          (update-in [:position/open]
                                                          #(merge % {:trade-transaction/type       :buy
                                                                     :trade-transaction/holding-id hid}))
                                 close         (update-in [:position/close]
                                                          #(merge % {:trade-transaction/type       :sell
                                                                     :trade-transaction/holding-id hid
                                                                     :trade-transaction/quantity   quantity}))
                                 (nil? status) (assoc :position/status :open))
        {:keys [send-message]} (fn-get-ctx)]
      (try
        (if-let [v (model/validate :position pos)]
          (do
            (send-message
             [:holding.command/save-position!-result
              {:error       v
               :type        :failed-validation
               :description "Invalid position"}]))
          (send-message
           [:holding.command/save-position!-result
            {:result (-> pos
                         fn-repo-save!
                         (select-keys [:position/creation-id :position/id :position/holding-position-id]))
             :type   :success}]))
        (catch Exception e
          (let [err-msg "Error saving position"]
            (errorf e err-msg)
            (send-message
             [:holding.command/save-position!-result
              {:error       (ex-data e)
               :description (str err-msg ": " (ex-message e))
               :type        :error}]))))))
