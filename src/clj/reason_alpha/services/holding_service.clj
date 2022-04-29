(ns reason-alpha.services.holding-service
  (:require [clojure.core.async :as async  :refer (<! go-loop)]
            [malli.core :as m]
            [outpace.config :refer [defconfig]]
            [reason-alpha.integration.eod-api-client :as eod-api-client]
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
                              (:position/holding-position-id %)))))
        sub-positions (-> positions
                          (lens/view
                           (lens/only
                            :position/holding-position-id)))
        holding-pos   (when holding-pos
                        (portfolio-management/stop-loss-amount holding-pos
                                                               sub-positions))
        sub-positions (if (seq sub-positions)
                        (map portfolio-management/stop-loss-amount
                             sub-positions)
                        [])]
    (cond-> []
      (seq sub-positions) (into sub-positions)
      holding-pos         (conj holding-pos)
      :else               [])))

(defn- assoc-close-prices-fn [fn-repo-get-acc-by-uid & [{:keys [batch-size]}]]
  (fn [account-id positions]
    (let [api-token     (-> account-id
                            fn-repo-get-acc-by-uid
                            :account/subscriptions
                            :subscription/eod-historical-data
                            :api-token)
          tickers       (->> positions
                             (map (fn [{:keys [holding-id eod-historical-data]}]
                                    [holding-id eod-historical-data]))
                             dedupe)
          prices        (fn-quote-live-prices api-token tickers {:batch-size (or batch-size 2)})
          positions     (->> prices
                             (pmap #(deref %))
                             (mapcat identity)
                             (mapcat (fn [{price-hid   :holding-id
                                           price-close :price-close}]
                                       (->> positions
                                            (filter (fn [{:keys [holding-id]}]
                                                      (= holding-id price-id)))
                                            (map #(assoc % :close-price price-close))))))
          #_#_positions (->> positions
                             (map (fn [{:keys [holding-id] :as pos}]
                                    (if-let [price (some #(if (= holding-id
                                                                 (:holding-id %))
                                                            (:price-close %))
                                                         prices)]
                                      (assoc pos :close-price price)
                                      pos))))]
      positions)))

(defn get-holding-positions-fn
  [fn-repo-get-holding-positions fn-repo-get-acc-by-uid fn-get-ctx]
  (let [fn-assoc-close-prices (assoc-close-prices-fn fn-repo-get-acc-by-uid)]
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

(defn get-holdings-positions-fn
  [fn-repo-get-holdings-positions fn-repo-get-acc-by-uid fn-get-ctx]
  (let [fn-assoc-close-prices (assoc-close-prices-fn fn-repo-get-acc-by-uid)]
    (fn [& [{:keys [account-id broadcast?]}]]
      (let [{send-message :send-message
             :as          ctx} (fn-get-ctx)
            acc-id             (or account-id
                                   (get-in ctx [:user-account :account/id]))
            positions          (->> {:account-id acc-id
                                     :role       (if account-id
                                                   :system
                                                   :member)}
                                    fn-repo-get-holdings-positions
                                    ;; For now all prices must be live
                                    (map #(assoc % :close-estimated? true))
                                    (group-by (fn [{:keys [position-id holding-position-id]}]
                                                (or holding-position-id
                                                    position-id)))
                                    (mapcat (fn [[_ hs]]
                                              (aggregate-holding-positions hs)))
                                    (as-> p (fn-assoc-close-prices acc-id p)))]
        (when broadcast? (swap! *broadcast-holdings-positions conj acc-id))
        (send-message
         [:holding.query/get-holdings-positions-result {:result positions
                                                        :type   :success}])))))


;; TODO: Deprecate this once `get-holdings-positions` is complete
(defn get-positions
  [fn-repo-get-positions fn-get-ctx]
  (let [{send-message         :send-message
         {acc-id :account/id} :user-account} (fn-get-ctx)
        ents                                 (->> acc-id
                                                  fn-repo-get-positions
                                                  ;; For now all prices must be live
                                                  (map #(assoc % :close-estimated? true)))]
    (swap! *quote-price-users conj acc-id)
    (send-message
     [:position.query/get-positions-result {:result ents
                                            :type   :success}])))

(defn broadcast-prices
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
                                   (filter #(some #{%} uids)))))]

            (doseq [acc-id @*broadcast-holdings-positions]
              (fn-get-holdings-positions {:account-id acc-id
                                          :broadcast? true}))

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
      ;;(println "Broadcast prices " i)
      (<! (async/timeout quote-interval))
      (broadcast! i)
      (recur (inc i)))))

(comment
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
