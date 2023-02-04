(ns reason-alpha.services.holding-service
  (:require [clojure.core.async :as async  :refer (<! go-loop close!)]
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

(comment
  (defn blah [& {:keys [test1 test2]}]
    [test1 test2])

  (blah :test1 "11111" :test2 "22222")

  (let [cs {:open-total {:function "quantity * open-price", :use [:quantity :open-price]},
            :profit-loss-amount
            {:function "(quantity * close-price) - open-total",
             :use      [:quantity :close-price],
             :require  [:open-total]},
            :stop-loss-percent
            {:function "TPERCENT(stop-loss/(quantity * open-price))",
             :use      [:stop-loss :quantity :open-price]}}
        p  [{:trade-pattern        [#uuid "018153ca-e7aa-3ee1-ad47-1b02e8eba24f" "Pullback"],
             :holding              [#uuid "01844716-6bbf-1097-1700-23db8db9af42" "ASML"],
             :open-price           222.0,
             :open-time            #inst "2022-11-02T00:00:00.000-00:00",
             :stop-loss            -4884.0,
             :stop                 0.0,
             :position-creation-id #uuid "84d374e9-0abe-4d92-9abe-6448c740dd65",
             :status               :closed,
             :close-price          93.91,
             :position-id          #uuid "0184b9ee-6b90-aabd-8c1e-b5a074741e52",
             :holding-id           #uuid "01844716-6bbf-1097-1700-23db8db9af42",
             :quantity             22,
             :eod-historical-data  "ASML.AS",
             :long-short           [:long ""]}]]
    (common/compute p {:computations cs}))

  )

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
