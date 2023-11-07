(ns reason-alpha.integration.market-data.services.equity-service
  (:require [reason-alpha.data.xtdb :as xtdb]
            [reason-alpha.integration.market-data.data.equity-repository :as repo]
            [reason-alpha.integration.market-data.integration.eod-api-client :as eodhd]
            [reason-alpha.utils :as utils]
            [tick.core :as tick]))

(def ^:dynamic *fn-repo-get-prices*
  repo/get-prices*) ;; TODO: Change to cached version

(def ^:dynamic *fn-quote-latest-intraday-prices*
  eodhd/quote-latest-intraday-prices)

(def ^:dynamic *fn-quote-historic-prices*
  eodhd/quote-historic-prices)

(declare save-position-prices)

(def ^:dynamic *fn-save-position-prices*
  save-position-prices)

(defn- type+date-range
  [& {:keys [symbol-ticker time]}]
  (let [today    (utils/time-at-beginning-of-day (tick/now))
        t        (or time (tick/now))
        date     (utils/time-at-beginning-of-day t)
        type     (if (= today date) :intraday #_else :historic)
        dt-range (if (= type :historic)
                   (utils/quarter-bounds t)
                   #_else
                   (let [n (tick/now)]
                     [(utils/time-at-beginning-of-day n)
                      (utils/time-at-end-of-day n)]))]
    {:symbol-ticker symbol-ticker
     :date-range    dt-range
     :type          type}))

(defn- save-position-prices
  [idx-retrieved-prs]
  (future
    (repo/save! (vals idx-retrieved-prs))))

(defn get-position-prices
  [& {:keys [positions api-token]}]
  (let [today                     (utils/time-at-beginning-of-day (tick/now))
        {without-ps :without-prices
         with-ps    :with-prices} (reduce
                                   (fn [r {:keys [open-price close-price] :as p}]
                                     (if (and open-price close-price)
                                       (update r :with-prices #(conj (or % []) p))
                                       #_else (update r :without-prices #(conj (or % []) p))))
                                   {}
                                   positions)
        types+date-ranges         (->> without-ps
                                       (pmap (fn [{:keys [eodhd close-price open-price
                                                          open-date close-date
                                                          position-id position-creation-id]
                                                   :as   p}]
                                               (cond-> []
                                                 (nil? open-price)
                                                 , (conj (type+date-range
                                                          :symbol-ticker eodhd
                                                          :time open-date))
                                                 (nil? close-price)
                                                 , (conj (type+date-range
                                                          :symbol-ticker eodhd
                                                          :time close-date)))))
                                       (apply concat))
        {stored :stored
         {not-stor-intrd :intraday
          not-stor-hist  :historic}
         :not-stored}             (->> types+date-ranges
                                       (reduce
                                        (fn [r {dr  :date-range
                                                st  :symbol-ticker
                                                t   :type
                                                :as price-info}]
                                          (let [db-prs       (*fn-repo-get-prices*
                                                              {:type          t
                                                               :date-range    dr
                                                               :symbol-ticker st})
                                                store-status (if (seq db-prs) :stored
                                                                 #_else :not-stored)]
                                            (update-in r [store-status t]
                                                       #(conj (or % #{})
                                                              (if (= t :intraday)
                                                                st #_else price-info)))))
                                        {}))
        *fetched-intrad           (*fn-quote-latest-intraday-prices*
                                   :symbol-tickers not-stor-intrd
                                   :api-token api-token)
        idx-retrieved-prs         (-> (fn [{t   :type
                                            dr  :date-range
                                            st  :symbol-ticker
                                            :as price-info}]
                                        (*fn-quote-historic-prices*
                                         :symbol-ticker st
                                         :date-range dr
                                         :api-token api-token))
                                      (pmap not-stor-hist)
                                      (conj *fetched-intrad)
                                      (as-> *f (pmap
                                                (fn [*fetched]
                                                  ;; TODO: Handle/log errors
                                                  (let [{r   :result
                                                         err :error
                                                         :as x} @*fetched
                                                        r-items (if (:date-range r)
                                                                  ;; Historic
                                                                  (:prices r)
                                                                  #_else ;; Intraday
                                                                  (->> r
                                                                       (map #(when (= (:type %) :success)
                                                                               (:result %)))
                                                                       (remove nil?)))]
                                                    r-items)) *f))
                                      (as-> x (apply concat x))
                                      (concat stored)
                                      (as-> x (pmap
                                               (fn [{:price/keys [type time symbol-ticker] :as p}]
                                                 (if (= type :intraday)
                                                   [[symbol-ticker type] p]
                                                   #_else [[symbol-ticker type time] p])) x))
                                      (as-> x (into {} x)))
        _                         (*fn-save-position-prices* idx-retrieved-prs)
        complemented-ps           (->> without-ps
                                       (pmap
                                        (fn [{:keys [open-price open-date close-price
                                                     close-date eodhd]
                                              :as   pos}]
                                          (cond-> pos
                                            (and (nil? open-price)
                                                 (nil? open-date))
                                            , (assoc :open-price
                                                     (-> idx-retrieved-prs
                                                         (get [eodhd :intraday])
                                                         :price/close))

                                            (and (nil? open-price)
                                                 (utils/today? open-date))
                                            , (assoc :open-price
                                                     (-> idx-retrieved-prs
                                                         (get [eodhd :intraday])
                                                         :price/close))

                                            (and (nil? open-price)
                                                 open-date
                                                 (not (utils/today? open-date)))
                                            , (assoc :open-price
                                                     (-> idx-retrieved-prs
                                                         (get [eodhd :historic open-date])
                                                         :price/close))

                                            (and (nil? close-price)
                                                 (nil? close-date))
                                            , (assoc :close-price
                                                     (-> idx-retrieved-prs
                                                         (get [eodhd :intraday])
                                                         :price/close))

                                            (and (nil? close-price)
                                                 (utils/today? close-date))
                                            , (assoc :close-price
                                                     (-> idx-retrieved-prs
                                                         (get [eodhd :intraday])
                                                         :price/close))

                                            (and (nil? close-price)
                                                 close-date
                                                 (not (utils/today? close-date)))
                                            , (assoc :close-price
                                                     (-> idx-retrieved-prs
                                                         (get [eodhd :historic close-date])
                                                         :price/close)))))
                                       (concat with-ps))]
    complemented-ps))

(comment

  (require '[reason-alpha.data.xtdb :as xtdb]
           '[reason-alpha.model.common :as common]
           '[reason-alpha.infrastructure.auth :as auth]
           '[reason-alpha.integration.market-data.data.equity-repository :as repo]
           '[reason-alpha.data.model :as data.model :refer [DataBase]]
           '[tick.alpha.interval :as tick.i])


  (let [db-nm      "dev-market-data"
        data-dir   "data/market-data"
        fn-authz   auth/authorize
        fn-get-ctx common/get-context
        db         (xtdb/db :data-dir data-dir
                            :db-name db-nm
                            :fn-get-ctx fn-get-ctx
                            :fn-authorize fn-authz)]
    (data.model/connect db)
    (def db db))

  (let [ps [{:eodhd                           "EL"
             :open-date                       #inst "2023-08-24T00:00:00.000-00:00",
             :open-price                      nil,
             :open-total-acc-currency         nil,
             :close-price                     763.7,
             :trade-pattern
             [#uuid "018a2c43-7a96-11a5-ab21-06f37976bbf8" "Breakout"],
             :target-profit-acc-currency      nil,
             :stop-loss-acc-currency          nil,
             :target-profit                   nil,
             :holding                         [#uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197" "Adyen"],
             :profit-loss-amount              -21.729999999999563,
             :open-total                      31333.43,
             :profit-loss-percent             "-0.07%",
             :stop-loss                       -31333.43,
             :holding-currency                :EUR,
             :stop                            0,
             :stop-loss-percent               "-100.0%",
             :position-creation-id            #uuid "d561b5c7-57ab-49b0-84ce-2d04b78f588c",
             :status                          :open,
             :position-id                     #uuid "018a2cac-c474-3ec7-962c-b5b285877385",
             :holding-position-id             #uuid "018a2caa-ba6e-c9a5-8d51-38553003af1f",
             :holding-id                      #uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197",
             :quantity                        41,
             :profit-loss-amount-acc-currency nil,
             :long-short                      [:long ""],
             :target-profit-percent           nil}
            {:eodhd                           "SOL.JSE"
             :open-date                       #inst "2023-03-24T00:00:00.000-00:00",
             :open-total-acc-currency         nil,
             :open-price                      nil,
             :close-price                     nil,
             :trade-pattern
             [#uuid "018a2c43-7a96-11a5-ab21-06f37976bbf8" "Breakout"],
             :target-profit-acc-currency      nil,
             :stop-loss-acc-currency          nil,
             :target-profit                   nil,
             :holding                         [#uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197" "Sasol"],
             :profit-loss-amount              -21.729999999999563,
             :open-total                      31333.43,
             :profit-loss-percent             "-0.07%",
             :stop-loss                       -31333.43,
             :holding-currency                :EUR,
             :stop                            0,
             :stop-loss-percent               "-100.0%",
             :position-creation-id            #uuid "cf9da077-0d0c-40d6-b570-f3edd056ca79",
             :status                          :open,
             :position-id                     #uuid "4b463c08-c0ce-4178-b5b8-1ebce5b7e53a",
             :holding-position-id             #uuid "018a2caa-ba6e-c9a5-8d51-38553003af1f",
             :holding-id                      #uuid "018a26c9-4b50-9f3f-d4ae-0206dd209197",
             :quantity                        5,
             :profit-loss-amount-acc-currency nil,
             :long-short                      [:long ""],
             :target-profit-percent           nil}
            {:eodhd                           "ADYEN.AS"
             :open-date                       #inst "2023-08-24T00:00:00.000-00:00",
             :open-total-acc-currency         nil,
             :open-price                      764.23,
             :close-price                     773.4,
             :target-profit-acc-currency      nil,
             :stop-loss-acc-currency          nil,
             :target-profit                   nil,
             :holding                         [#uuid "018a465e-82bd-de65-2623-02bb30e1a1f6" "Multiple"],
             :profit-loss-amount              375.9708007812478,
             :open-total                      31333.42919921875,
             :profit-loss-percent             "1.2%",
             :stop-loss                       -31333.42919921875,
             :holding-currency                :SGD,
             :stop                            0.0,
             :stop-loss-percent               "-100.0%",
             :sub-positions                   '(#uuid "d561b5c7-57ab-49b0-84ce-2d04b78f588c"),
             :position-creation-id            #uuid "3cee7b50-68a9-4fa3-b9eb-5464068ad465",
             :status                          :open,
             :close-date                      nil,
             :position-id                     #uuid "018a2caa-ba6e-c9a5-8d51-38553003af1f",
             :holding-id                      #uuid "018a465e-82bd-de65-2623-02bb30e1a1f6",
             :quantity                        41,
             :profit-loss-amount-acc-currency nil,
             :long-short                      [:hedged ""],
             :target-profit-percent           nil}]]
    #_(mapv #(into (sorted-map) %) ps)
    (get-position-prices :positions ps
                         :api-token eodhd/dev-api-token))



  (-> "2023-01"
      tick/year-month
      t.i/bounds
      :tick/beginning)

  (-> "2023-03"
      tick/year-month
      t.i/bounds
      :tick/end
      (tick/<< (tick/new-duration 1 :days))
      (tick/in "UTC")
      tick/inst)

  (-> (tick/inst) tick/instant tick/long)

  (let [q-start-month-nr (-> 1
                             dec
                             (* 3)
                             inc)
        q-end-month-nr   (+ q-start-month-nr 2)]
    [q-start-month-nr q-end-month-nr])


  )
