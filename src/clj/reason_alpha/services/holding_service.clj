(ns reason-alpha.services.holding-service
  (:require [clojure.core.async :as as :refer (<! <!! go-loop close!)]
            [malli.core :as m]
            [medley.core :as medley]
            [outpace.config :refer [defconfig]]
            [promesa.core :as p]
            [reason-alpha.infrastructure.message-processing :as msg-processing]
            [reason-alpha.integration.market-data.integration.exchangerate-host-api-client :as exchangerate]
            [reason-alpha.integration.market-data.integration.marketstack-api-client :as marketstack]
            [reason-alpha.model.accounts :as accounts]
            [reason-alpha.model.common :as common]
            [reason-alpha.model.core :as model]
            [reason-alpha.model.fin-instruments :as fin-instruments]
            [reason-alpha.model.portfolio-management :as portfolio-management]
            [reason-alpha.utils :as utils]
            [tick.core :as tick]
            [traversy.lens :as lens]))

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
  (let [holding                (if acc-id
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
          (utils/log ::save-holding! {:type        :error
                                      :error       e
                                      :description err-msg})
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
  (let [{acc-id :account/id
         :as x}   (fn-get-account)
        {:keys [send-message]} (fn-get-ctx)
        holdings               (fn-repo-get-positions
                                {:account-id acc-id})]
    (send-message
     [:holding.query/get-holdings-result {:result holdings
                                          :type   :success}])))

(defn get-holding-ids-with-positions-fn
  [fn-repo-get-holdings-with-positions]
  (fn [holding-ids]
      (->> holding-ids
           fn-repo-get-holdings-with-positions
           (map :holding-id))))

(comment
  (let [ps [{:open-total-acc-currency         nil,
             :trade-pattern
             [#uuid "018a2c43-7a96-11a5-ab21-06f37976bbf8" "Breakout"],
             :target-profit-acc-currency      nil,
             :stop-loss-acc-currency          nil,
             :target-profit                   nil,
             :holding                         [#uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197" "Adyen"],
             :profit-loss-amount              -21.729999999999563,
             :open-total                      31333.43,
             :open-price                      764.23,
             :profit-loss-percent             "-0.07%",
             :stop-loss                       -31333.43,
             :holding-currency                :EUR,
             :stop                            0,
             :marketstack                     "ADYEN.XAMS",
             :stop-loss-percent               "-100.0%",
             :position-creation-id            #uuid "d561b5c7-57ab-49b0-84ce-2d04b78f588c",
             :status                          :open,
             :close-price                     763.7,
             :position-id                     #uuid "018a2cac-c474-3ec7-962c-b5b285877385",
             :open-date                       #inst "2023-08-24T00:00:00.000-00:00",
             :holding-position-id             #uuid "018a2caa-ba6e-c9a5-8d51-38553003af1f",
             :holding-id                      #uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197",
             :quantity                        41,
             :profit-loss-amount-acc-currency nil,
             :long-short                      [:long ""],
             :target-profit-percent           nil}
            {:open-total-acc-currency         nil,
             :trade-pattern
             [#uuid "018a2c43-7a96-11a5-ab21-06f37976bbf8" "Breakout"],
             :target-profit-acc-currency      nil,
             :stop-loss-acc-currency          nil,
             :target-profit                   nil,
             :holding                         [#uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197" "Adyen"],
             :profit-loss-amount              -21.729999999999563,
             :open-total                      31333.43,
             :open-price                      764.23,
             :profit-loss-percent             "-0.07%",
             :stop-loss                       -31333.43,
             :holding-currency                :EUR,
             :stop                            0,
             :marketstack                     "ADYEN.XAMS",
             :stop-loss-percent               "-100.0%",
             :position-creation-id            #uuid "cf9da077-0d0c-40d6-b570-f3edd056ca79",
             :status                          :open,
             :close-price                     763.7,
             :position-id                     #uuid "4b463c08-c0ce-4178-b5b8-1ebce5b7e53a",
             :open-date                       #inst "2023-08-24T00:00:00.000-00:00",
             :holding-position-id             #uuid "018a2caa-ba6e-c9a5-8d51-38553003af1f",
             :holding-id                      #uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197",
             :quantity                        5,
             :profit-loss-amount-acc-currency nil,
             :long-short                      [:long ""],
             :target-profit-percent           nil}
            {:open-total-acc-currency         nil,
             :target-profit-acc-currency      nil,
             :stop-loss-acc-currency          nil,
             :target-profit                   nil,
             :holding                         [#uuid "018a465e-82bd-de65-2623-02bb30e1a1f6" "Multiple"],
             :profit-loss-amount              375.9708007812478,
             :open-total                      31333.42919921875,
             :open-price                      764.23,
             :profit-loss-percent             "1.2%",
             :stop-loss                       -31333.42919921875,
             :holding-currency                :SGD,
             :stop                            0.0,
             :stop-loss-percent               "-100.0%",
             :sub-positions                   '(#uuid "d561b5c7-57ab-49b0-84ce-2d04b78f588c"),
             :position-creation-id            #uuid "3cee7b50-68a9-4fa3-b9eb-5464068ad465",
             :status                          :open,
             :close-price                     773.4,
             :close-date                      nil,
             :position-id                     #uuid "018a2caa-ba6e-c9a5-8d51-38553003af1f",
             :open-date                       #inst "2023-08-24T00:00:00.000-00:00",
             :holding-id                      #uuid "018a465e-82bd-de65-2623-02bb30e1a1f6",
             :quantity                        41,
             :profit-loss-amount-acc-currency nil,
             :long-short                      [:hedged ""],
             :target-profit-percent           nil}]]
    (aggregate-holding-positions ps))

  )

#_(defn get-close-prices<
  [positions & {:keys [account batch-size]}]
  (when-let [access-key (-> account
                            :account/subscriptions
                            :subscription/marketstack
                            :access-key)]
    (let [result-out-chnl (as/chan)]
      (if (seq positions)
        (do
          (as/go
            (let [symbols           (->> positions
                                         (map (fn [{:keys [holding-id marketstack]}]
                                                (when (and holding-id marketstack)
                                                  {:symbol     marketstack
                                                   :holding-id holding-id})))
                                         (remove nil?)
                                         distinct)
                  quote-result-chnl (marketstack/quote-eod-share-prices
                                     access-key symbols :batch-size batch-size)
                  idx-hid->quote    (->> symbols
                                         (mapv (fn [_]
                                                 (let [{hid :holding-id
                                                        err :error
                                                        :as r} (as/<!! quote-result-chnl)]
                                                   [hid r])))
                                         (into {}))
                  pos-with-close-pr (->> positions
                                         (map (fn [{:keys [holding-id status position-id] :as p}]
                                                (let [{price :price-close
                                                       s     :symbol
                                                       err   :error} (get idx-hid->quote
                                                                          holding-id)]
                                                  (cond
                                                    err
                                                    , (utils/ignore
                                                       (errorf (str "Error occurred getting the share "
                                                                    "price of '%s' (holding-id %s)")
                                                               holding-id
                                                               (pr-str err)))

                                                    (#{:open} status)
                                                    , [position-id {:close-price price}]))))
                                         (into {}))]
              (as/>! result-out-chnl pos-with-close-pr)))
          (as/take 1 result-out-chnl))
        (do
          (as/close! result-out-chnl)
          result-out-chnl)))))

(defn get-close-prices*
  [positions & {:keys [account batch-size]}]
  (when-let [access-key (-> account
                            :account/subscriptions
                            :subscription/eodhd
                            :subscription-key)])
  (let [*result (p/deferred)]
    (msg-processing/send-msg
     {:msg/type  :equity.queries/get-position-prices
      :msg/value positions}
     :result-topic :equity.queries/get-position-prices-result
     :fn-receive-msg #(p/resolve! *result %))))

(defn get-fx-rates*
  [positions & {{acc-currency :account/currency} :account}]
  (p/resolved
   (pmap #(assoc %
                 :open-fx-rate 1.0
                 :close-fx-rate 1.0) positions)))

#_(defn get-fx-rates<
  [positions & {{acc-currency :account/currency} :account}]
  (let [result-out-chnl (as/chan)]
    (if-let [currency-conversions (and acc-currency
                                       (->> positions
                                            (mapcat (fn [{:keys [holding-currency open-time close-time]}]
                                                      [{:from holding-currency
                                                        :to   acc-currency
                                                        :date close-time}
                                                       {:from holding-currency
                                                        :to   acc-currency
                                                        :date open-time}]))
                                            distinct
                                            seq))]
      (do
        (as/go
          (let [fxrate-result-chnl         (exchangerate/convert currency-conversions)
                idx-currency-pair->fx-rate (->> currency-conversions
                                                (map
                                                 (fn [_]
                                                   (let [{:keys [fx-rate]
                                                          :as   c} (as/<!! fxrate-result-chnl)]
                                                     [(dissoc c :fx-rate) fx-rate])))
                                                (into {}))
                pos-with-fx-rates          (->> positions
                                                (map
                                                 (fn [{:keys [holding-currency position-id
                                                              open-time close-time]
                                                       :as   p}]
                                                   (let [open-dte  (tick/date (or open-time (tick/now)))
                                                         close-dte (tick/date (or close-time (tick/now)))
                                                         open-k    {:from holding-currency
                                                                    :to   acc-currency
                                                                    :date open-dte}
                                                         close-k   {:from holding-currency
                                                                    :to   acc-currency
                                                                    :date close-dte}
                                                         open-fxr  (get idx-currency-pair->fx-rate open-k)
                                                         close-fxr (get idx-currency-pair->fx-rate close-k)
                                                         fxrs      (cond-> {}
                                                                     open-fxr  (assoc :open-fx-rate open-fxr)
                                                                     close-fxr (assoc :close-fx-rate close-fxr))]
                                                     [position-id fxrs])))
                                                (into {}))]
            (as/>! result-out-chnl pos-with-fx-rates)))
        (as/take 1 result-out-chnl))
      #_else
      (do
        (as/close! result-out-chnl)
        result-out-chnl))))

;; TODO: This can be made completely generic, by allowing the primary key,
;;       in this case `:position-id` to be specified.
(defn- assoc-market-data-fn
  [fn-repo-get-acc-by-aid fns-market-data & {:keys [time-limit batch-size]}]
  (fn assoc-market-data [account-id positions]
    (let [acc        (fn-repo-get-acc-by-aid account-id)
          time-limit (or time-limit 10000)] ;; 10 seconds
      (if acc
        (let [ps (-> (fn [fn-get-market-data]
                       (-> positions
                           (fn-get-market-data :batch-size batch-size
                                               :account acc)
                           (p/timeout time-limit)
                           (p/then #(p/resolved %))
                           (p/catch
                               #(let [err-msg "Market data function timed out or failed with an error"]
                                  (utils/log ::assoc-market-data-fn
                                             {:type        :error
                                              :description err-msg
                                              :error       (ex-info err-msg
                                                                    {:exception %})})))))
                     (pmap fns-market-data)
                     (p/all)
                     deref
                     (conj positions))
              ps (->> ps
                      (mapcat #(identity %))
                      (reduce (fn [pid->pos {:keys [position-id] :as pos}]
                                (if-let [p (get pid->pos position-id)]
                                  (->> pos (merge p) (assoc pid->pos position-id))
                                  #_else (assoc pid->pos position-id pos)))
                              {})
                      vals)]
          {:result ps
           :type   :success})
        #_else
        {:result positions
         :type   :success}))))

(def postn-comps (common/computations portfolio-management/PositionDto))

(defn- complement-positions
  [fn-repo-get-acc-by-aid fns-market-data acc-id positions]
  (let [fn-assoc-market-data (assoc-market-data-fn fn-repo-get-acc-by-aid
                                                   fns-market-data)
        {:keys [holding-position
                positions]
         :as   r}            (portfolio-management/idx-position-type positions)
        {positions :result
         t         :type
         :as       r}        (fn-assoc-market-data acc-id positions)
        {positions :result
         t         :type
         :as       r}        (if (= :success t)
                               (common/compute positions {:computations postn-comps})
                               r)
        {holding-position :result
         t                :type
         :as              r} (when (and (= :success t) holding-position)
                               (portfolio-management/aggregate-holding-position
                                holding-position positions))
        positions            (if (= t :success)
                               (conj positions holding-position)
                               #_else positions)]
    {:result positions
     :type   :success}))

(defn get-holding-positions-fn
  [fn-repo-get-holding-positions fn-repo-get-acc-by-uid fns-market-data fn-get-ctx]
  (fn [{:keys [position/id position/holding-position-id]}]
    (let [pos-id                               (or holding-position-id id)
          {send-message         :send-message
           {acc-id :account/id} :user-account} (fn-get-ctx)
          result                               (->> pos-id
                                                    fn-repo-get-holding-positions
                                                    (complement-positions
                                                     fn-repo-get-acc-by-uid
                                                     fns-market-data
                                                     acc-id))]
      (send-message [:holding.query/get-holding-positions-result
                     result]))))

(def *broadcast-holdings-positions (atom #{}))

(defconfig price-quote-interval)

(defn get-holdings-positions
  [fn-repo-get-holdings-positions fn-repo-get-acc-by-uid fn-repo-get-acc-by-aid
   fns-market-data broadcast? {:keys [fn-get-ctx account-id send-message]}]
  (let [{send-msg :send-message
         :as      ctx} (when fn-get-ctx
                         (fn-get-ctx))
        {acc-id :account/id
         :as    acc}   (if account-id
                         (fn-repo-get-acc-by-aid account-id)
                         #_else
                         (-> ctx
                             (get-in [:user-account :account/user-id])
                             fn-repo-get-acc-by-uid))
        send-msg       (or send-msg
                           #(send-message acc-id %))
        gpositions     (->> {:account-id acc-id
                             :role       (if account-id
                                           :system
                                           :member)}
                            fn-repo-get-holdings-positions
                            (group-by (fn [{:keys [position-id holding-position-id]}]
                                        (or holding-position-id
                                            position-id))))]

    ;; After 1st request message, start broadcasting
    (when broadcast? (swap! *broadcast-holdings-positions conj acc-id))

    (doseq [[_gpos-id posns] gpositions]
      ;; TODO: REMOVE DEREF
      @(future
        (try
          (let [result (complement-positions
                        fn-repo-get-acc-by-aid
                        fns-market-data
                        acc-id
                        posns)]
            (send-msg
             [:holding.query/get-holdings-positions-result result]))
          (catch Exception e
            (let [err-msg "Error getting holdings positions"]
              (utils/log ::get-holdings-positions
                         {:type        :error
                          :description err-msg
                          :error       e})
              (send-msg
               [:holding.query/get-holdings-positions-result
                {:result-id   (utils/new-uuid)
                 :error       (ex-data e)
                 :title       err-msg
                 :description (ex-message e)
                 :type        :error}]))))))))

(defonce *broadcast? (atom true))
(def *holdings-positions-ch (atom nil))

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

    (reset!
     *holdings-positions-ch
     (go-loop [i 0]
       (println "Broadcast holdings positions " i)
       (<! (as/timeout quote-interval))
       (when @*broadcast?
         (broadcast! i)
         (recur (inc i)))))

    (.addShutdownHook (Runtime/getRuntime)
                      (Thread.
                       #(do (println (str "Shutting down broadcast "
                                          "holdings positions channel"))
                            (close! @*holdings-positions-ch))))))

(defn stop-broadcast-holdings-positions []
  (println "Stop broadcasting holdings positions")
  (reset! *broadcast? false))

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
      (if-let [v (model/validate :model/position pos)]
        (send-message
         [:holding.command/save-position!-result
          {:error       v
           :type        :failed-validation
           :description "Invalid position"}])
        (send-message
         [:holding.command/save-position!-result
          {:result (-> pos
                       fn-repo-save!
                       (select-keys [:position/creation-id :position/id :position/holding-position-id]))
           :type   :success}]))
        (catch Exception e
          (let [err-msg "Error saving position"]
            (utils/log {:type        :error
                        :description err-msg
                        :error       e})
            (send-message
             [:holding.command/save-position!-result
              {:error       (ex-data e)
               :description (str err-msg ": " (ex-message e))
               :type        :error}]))))))
