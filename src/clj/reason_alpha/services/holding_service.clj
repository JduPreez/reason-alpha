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
  (let [instr                  (if acc-id
                                 instrument
                                 (->> (fn-get-account)
                                      :account/id
                                      (assoc instrument :holding/account-id)))
        {:keys [send-message]} (fn-get-ctx)]
    (try
      (send-message
       [:holding.command/save!-result
        {:result (-> instr
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

(defn get-holding [fn-repo-get1 fn-get-ctx {:keys [instrument-id]}]
  (let [{:keys [send-message]} (fn-get-ctx)
        instr                  (fn-repo-get1 instrument-id)]
    (send-message
     [:holding.query/get-holding-result {:result instr
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
  (let [holding-pos   (-> positions
                          (lens/view-single
                           (lens/only
                            #(nil?
                              (:holding-position-id %)))))
        sub-positions (-> positions
                          (lens/view
                           (lens/only
                            :holding-position-id)))
        holding-pos   (when holding-pos
                        (portfolio-management/assoc-stop-total-loss
                         holding-pos sub-positions))
        sub-positions (when (seq sub-positions)
                        (map portfolio-management/assoc-stop-total-loss
                             sub-positions))]
    (cond-> []
      (seq sub-positions) (into sub-positions)
      holding-pos         (conj holding-pos))))

(defn- assoc-close-prices-fn [fn-repo-get-acc-by-uid fn-quote-live-prices account-id & [{:keys [batch-size]}]]
  (fn [positions]
    (println ::assoc-close-prices)
    (let [api-token         (-> account-id
                                fn-repo-get-acc-by-uid
                                :account/subscriptions
                                :subscription/eod-historical-data
                                :api-token)
          tickers           (->> positions
                                 (map (fn [{:keys [holding-id eod-historical-data]}]
                                        [holding-id eod-historical-data]))
                                 dedupe)
          prices            (fn-quote-live-prices api-token tickers {:batch-size (or batch-size 2)})
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
                                                          p)))))))
          ;; positions (->> positions
          ;;                    (map (fn [{:keys [holding-id] :as pos}]
          ;;                           (if-let [price (some #(if (= holding-id
          ;;                                                        (:holding-id %))
          ;;                                                   (:price-close %))
          ;;                                                prices)]
          ;;                             (assoc pos :close-price price)
          ;;                             pos))))
          ]
      pos-with-close-pr)))

(defn get-holding-positions-fn
  [fn-repo-get-holding-positions fn-repo-get-acc-by-uid fn-quote-live-prices fn-get-ctx]
  (let [fn-assoc-close-prices (partial assoc-close-prices-fn fn-repo-get-acc-by-uid
                                       fn-quote-live-prices)]
    (fn [id]
      (let [{send-message         :send-message
             {acc-id :account/id} :user-account} (fn-get-ctx)
            positions                            (-> id
                                                     fn-repo-get-holding-positions
                                                     aggregate-holding-positions
                                                     (as-> p (fn-assoc-close-prices acc-id p)))]
        (send-message [:holding.query/get-holding-positions-result
                       {:result positions
                        :type   :success}])))))

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
                                                     fn-quote-live-prices
                                                     acc-id)
        positions             (->> {:account-id acc-id
                                    :role       (if account-id
                                                  :system
                                                  :member)}
                                   fn-repo-get-holdings-positions
                                   ;; For now all prices must be live
                                   ;;(map #(assoc % :close-estimated? true))
                                   (group-by (fn [{:keys [position-id holding-position-id]}]
                                               (or holding-position-id
                                                   position-id)))
                                   (mapcat (fn [[_ hs]]
                                             (aggregate-holding-positions hs)))
                                   fn-assoc-close-prices)]
    (when broadcast? (swap! *broadcast-holdings-positions conj acc-id))

    (send-msg
     [:holding.query/get-holdings-positions-result {:result positions
                                                    :type   :success}])))

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
                                          :account-id   acc-id}))

            ;; (doseq [acc-id qte-price-usrs
            ;;         :let   [api-token           (-> acc-id
            ;;                                         fn-repo-get-acc-by-uid
            ;;                                         :account/subscriptions
            ;;                                         :subscription/eod-historical-data
            ;;                                         :api-token)
            ;;                 tickers             (->> {:account-id acc-id
            ;;                                           :role       :system}
            ;;                                          fn-repo-get-positions
            ;;                                          (map (fn [{:keys [holding-id eod-historical-data]}]
            ;;                                                 [holding-id eod-historical-data])))
            ;;                 price-results       (fn-quote-live-prices api-token tickers {:batch-size 2})
            ;;                 fn-send-price-quote (fn [price-quotes]
            ;;                                       (send-message acc-id [:price/quotes price-quotes]))]]
            ;;   (doall
            ;;    (pmap #(fn-send-price-quote (deref %)) price-results)))
            ))]

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
            (clojure.pprint/pprint {::save-position! v})
            (send-message
             [:holding.command/save-position!-result
              {:error       v
               :type        :failed-validation
               :description "Invalid position"}]))
          (send-message
           [:holding.command/save-position!-result
            {:result (-> pos
                         fn-repo-save!
                         (select-keys [:position/creation-id :position/id]))
             :type   :success}]))
        (catch Exception e
          (let [err-msg "Error saving position"]
            (errorf e err-msg)
            (send-message
             [:holding.command/save-position!-result
              {:error       (ex-data e)
               :description (str err-msg ": " (ex-message e))
               :type        :error}]))))))

(comment
  (let [p '({:holding
             [#uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5"
              "AAA"],
             :open-price  23,
             :open-time
             #inst "2022-05-12T00:00:00.000-00:00",
             :stop        3.2,
             :position-creation-id
             #uuid "bdc2bfb6-dcd4-4674-bc91-dc56b0a93276",
             :status      :open,
             :close-price 33.3,
             :position-id
             #uuid "01808fb9-4f61-999f-6a3b-58f29b27acee",
             :quantity    22.44,
             :long-short  [:long ""]}
            {:trade-pattern
             [#uuid "01800865-9069-63c7-9c6c-4d24cdcefc9a"
              "Breakout"],
             :holding
             [#uuid "018004bb-227c-d6de-69ac-bc1eab688ab5"
              "BBB"],
             :open-price  "333",
             :open-time
             #inst "2022-04-06T00:00:00.000-00:00",
             :stop        "3333",
             :position-creation-id
             #uuid "74e921b2-fe79-454d-b47c-08c43b298019",
             :close-price 36.85,
             :position-id
             #uuid "0180098b-e65e-d7ce-645b-41eef737fa0c",
             :holding-position-id
             #uuid "018008e1-a0db-2638-b2e2-4e9f0e332d11",
             :quantity    "3333",
             :long-short  [:long ""]}
            {:trade-pattern
             [#uuid "0180088d-aa18-6709-de16-4d2e56126947"
              "zzzzzz"],
             :holding
             [#uuid "018004b9-3a7f-df48-4c96-c63d6aea78b5"
              "AAA"],
             :open-price  "23",
             :open-time
             #inst "2022-04-20T00:00:00.000-00:00",
             :stop        "2342",
             :position-creation-id
             #uuid "0d4a7fdf-5ab0-4d08-a35a-c5b23fa46c6e",
             :close-price 83.33,
             :position-id
             #uuid "018008e1-a0db-2638-b2e2-4e9f0e332d11",
             :quantity    "454",
             :long-short  [:long ""]})]
    (->> p
         (group-by (fn [{:keys [position-id holding-position-id]}]
                     (or holding-position-id
                         position-id)))
         (mapcat (fn [[_ hs]]
                   (aggregate-holding-positions hs)))))



  (let [id        2
        positions [{:position/id 1}
                   {:position/id                  2
                    :position/holding-position-id 1}
                   {:position/id                  3
                    :position/holding-position-id 1}
                   {:position/id 4}
                   {:position/id                  5
                    :position/holding-position-id 4}
                   {:position/id                  6
                    :position/holding-position-id 4}]]
    (->> positions
         (group-by (fn [{:keys [position/id position/holding-position-id]}]
                    (or holding-position-id
                        id)))))

  (let [x {1
           [#:position{:id 1}
            #:position{:id 2, :holding-position-id 1}
            #:position{:id 3, :holding-position-id 1}],
           4
           [#:position{:id 4}
            #:position{:id 5, :holding-position-id 4}
            #:position{:id 6, :holding-position-id 4}]}]
    (map (fn [y] y) x))


  )
