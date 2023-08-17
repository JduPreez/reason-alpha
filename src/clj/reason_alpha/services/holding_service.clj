(ns reason-alpha.services.holding-service
  (:require [clojure.core.async :as async  :refer (<! <!! go-loop close!)]
            [malli.core :as m]
            [outpace.config :refer [defconfig]]
            [reason-alpha.integration.marketstack-api-client :as marketstack]
            [reason-alpha.integration.exchangerate-host-api-client :as exchangerate]
            [reason-alpha.model.accounts :as accounts]
            [reason-alpha.model.common :as common]
            [reason-alpha.model.core :as model]
            [reason-alpha.model.fin-instruments :as fin-instruments]
            [reason-alpha.model.portfolio-management :as portfolio-management]
            [reason-alpha.utils :as utils]
            [taoensso.timbre :as timbre :refer (errorf)]
            [traversy.lens :as lens]
            [clojure.core.async :as as]))

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

(defn- aggregate-holding-positions [positions]
  (let [comps          (common/computations portfolio-management/PositionDto)
        holding-pos    (-> positions
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
                         :always             (common/compute {:computations comps}))]
    pos-with-comps))

(defn- get-close-prices<
  [positions & {:keys [account batch-size]}]
  (when-let [access-key (-> account
                            :account/subscriptions
                            :subscription/marketstack
                            :access-key)]
    (let [result-out-chnl (as/chan)]
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
                                     (map (fn [_]
                                            (let [{hid :holding-id
                                                   err :error
                                                   :as r} (as/<! quote-result-chnl)]
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
                                                , (utils/do!
                                                   (errorf (str "Error occurred getting the share "
                                                                "price of '%s' (holding-id %s)")
                                                           holding-id
                                                           (pr-str err)))

                                                (#{:open} status)
                                                , [position-id {:close-price price}]))))
                                     (into {}))]
          pos-with-close-pr))
      (as/take 1 result-out-chnl))))

(defn- get-fx-rates<
  [positions & {{acc-currency :account/currency} :account}]
  (when-let [currency-conversions (and acc-currency
                                       (->> positions
                                            (map (fn [{:keys [holding-currency]}]
                                                   {:from holding-currency
                                                    :to   acc-currency}))
                                            distinct
                                            seq))]
    (let [result-out-chnl (as/chan)]
          (as/go
            (let [fxrate-result-chnl         (exchangerate/convert currency-conversions)
                  idx-currency-pair->fx-rate (->> currency-conversions
                                                  (map (fn [_]
                                                         (let [{:keys [fx-rate]
                                                                :as   c} (as/<! fxrate-result-chnl)]
                                                           [(dissoc c :fx-rate) fx-rate])))
                                                  (into {}))
                  pos-with-fx-rate           (->> positions
                                                  (map (fn [{:keys [holding-currency position-id]
                                                             :as   p}]
                                                         (let [k   {:from holding-currency
                                                                    :to   acc-currency}
                                                               fxr (get idx-currency-pair->fx-rate k)]
                                                           (when fxr
                                                             [position-id {:fx-rate fxr}]))))
                                                  (into {}))]
              (as/>! result-out-chnl pos-with-fx-rate)))
          (as/take 1 result-out-chnl))))

;; TODO: This can be made completely generic, by allowing the primary key,
;;       in this case `:position-id` to be specified.
(defn- assoc-market-data-fn
  [fn-repo-get-acc-by-uid fns-market-data & {:keys [batch-size]}]
  (fn assoc-market-data [account-id positions]
    (if-let [acc (fn-repo-get-acc-by-uid account-id)]
      (loop [result-chnls (pmap #(% positions
                                    :batch-size batch-size
                                    :account acc) fns-market-data)
             ps           positions]
        (if-let  [[pid->market-data c] (and (seq result-chnls)
                                            (as/alts!! result-chnls))]
          (let [ps (map (fn [{:keys [position-id] :as p}]
                          (if-let [p-md (get pid->market-data position-id)]
                            (merge p p-md)
                            p))
                        ps)]
            (recur (remove #(= c %) result-chnls) ps))
          ps))
      positions)))


#_(defn- assoc-market-data-fn
  [fn-repo-get-acc-by-uid fn-quote-eod-sharem-prices & {:keys [batch-size]}]
  (fn assoc-market-data [account-id positions]
    (if-let [access-key (-> account-id
                            fn-repo-get-acc-by-uid
                            :account/subscriptions
                            :subscription/marketstack
                            :access-key)]
      (let [symbols           (->> positions
                                   (map (fn [{:keys [holding-id marketstack]}]
                                          (when (and holding-id marketstack)
                                            {:symbol     marketstack
                                             :holding-id holding-id})))
                                   (remove nil?)
                                   distinct)
            result-out-chnl   (fn-quote-eod-share-prices access-key symbols :batch-size batch-size)
            idx-hid->quote    (->> symbols
                                   (map (fn [_]
                                          (let [{hid :holding-id
                                                 err :error
                                                 :as r} (<!! result-out-chnl)]
                                            [hid r])))
                                   (into {}))
            pos-with-close-pr (->> positions
                                   (mapv (fn [{:keys [holding-id status] :as p}]
                                           (let [{price :price-close
                                                  s     :symbol
                                                  err   :error} (get idx-hid->quote
                                                                     holding-id)]
                                             (cond
                                               err
                                               , (do
                                                   (errorf (str "Error occurred getting the share "
                                                                "price of '%s' (holding-id %s)")
                                                           holding-id
                                                           (pr-str err))
                                                   p)

                                               (#{:open} status)
                                               , (assoc p :close-price price)

                                               :else
                                               , p)))))]
        pos-with-close-pr)
      #_else
      positions)))

(defn get-holding-positions-fn
  [fn-repo-get-holding-positions fn-repo-get-acc-by-uid fns-market-data fn-get-ctx]
  (let [fn-assoc-close-prices (assoc-market-data-fn fn-repo-get-acc-by-uid
                                                    fns-market-data)]
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
  [fn-repo-get-holdings-positions fn-repo-get-acc-by-uid fns-market-data
   broadcast? {:keys [fn-get-ctx account-id send-message]}]
  (let [{send-msg :send-message
         :as      ctx}        (when fn-get-ctx
                                (fn-get-ctx))
        acc-id                (or account-id
                                  (and ctx
                                       (get-in ctx [:user-account :account/id])))
        send-msg              (or send-msg
                                  #(send-message acc-id %))
        fn-assoc-close-prices (assoc-market-data-fn fn-repo-get-acc-by-uid
                                                    fns-market-data)
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
       (<! (async/timeout quote-interval))
       (broadcast! i)
       (when @*broadcast?
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
