(ns reason-alpha.model
  (:require [clojure.pprint :as pprint]
            [integrant.core :as ig]
            [malli.instrument :as malli.instr]
            [reason-alpha.data.model :as data.model :refer [DataBase]]
            [reason-alpha.data.repositories.account-repository :as account-repo]
            [reason-alpha.data.repositories.holding-repository :as holding-repo]
            [reason-alpha.data.repositories.trade-pattern-repository :as trade-pattern-repo]
            [reason-alpha.data.xtdb :as xtdb]
            [reason-alpha.infrastructure.auth :as auth]
            [reason-alpha.infrastructure.server :as server]
            [reason-alpha.integration.fake-eod-api-client :as eod]
            [reason-alpha.integration.marketstack-api-client :as marketstack]
            [reason-alpha.model.common :as common]
            [reason-alpha.services.account-service :as account-svc]
            [reason-alpha.services.common :as svc.common]
            [reason-alpha.services.holding-service :as holding-svc]
            [reason-alpha.services.model-service :as model-svc]
            [reason-alpha.services.trade-pattern-service :as trade-pattern-svc]
            [traversy.lens :as tl]))

(defn handlers [aggregates]
  (-> aggregates
      (tl/update
       tl/all-entries
       (fn [[aggr-k {cmds  :commands
                     qries :queries}]]
         (letfn [(to-ns-keys [{:keys [commands queries]}]
                   (-> commands
                       (or queries {})
                       (tl/update
                        tl/all-keys
                        #(-> aggr-k
                             name
                             (str "." (if commands "command" "query") "/" (name %))
                             keyword))))]
           (merge (to-ns-keys {:commands cmds})
                  (to-ns-keys {:queries qries})))))))

(defmethod ig/init-key ::db [_ {:keys [fn-authorize fn-get-ctx]}]
  (let [db (xtdb/db fn-get-ctx fn-authorize)]
    (data.model/connect db)
    db))

(defmethod ig/init-key ::account-svc [_ {:keys [db]}]
  (let [fn-repo-save!      #(account-repo/save! db %)
        fn-repo-get-by-uid #(account-repo/get-by-user-id db %)]
    {:fn-get-account   #(account-svc/get-account common/get-context
                                                 fn-repo-get-by-uid)
     :fn-save-account! #(account-svc/save! fn-repo-get-by-uid
                                           fn-repo-save! %)}))

(defmethod ig/halt-key! ::db [_ db]
  (data.model/disconnect db))

(defmethod ig/init-key ::aggregates [_ {db   :db
                                        #_#_ {:keys [fn-get-account]} :account-svc}]
  (let [fn-repo-get-acc-by-uid #(account-repo/get-by-user-id db %)
        fn-get-account         (partial account-svc/get-account
                                        common/get-context fn-repo-get-acc-by-uid)]
    {:account
     {:commands {:save-any! (as-> db d
                              (partial account-repo/save! d)
                              (partial account-svc/save-any! fn-repo-get-acc-by-uid d))
                 :save!     (as-> db d
                              (partial account-repo/save! d)
                              (partial account-svc/save! fn-get-account d))}
      :queries  {:get1 (as-> db d
                         (partial account-repo/get1 d)
                         (partial account-svc/get1
                                  common/get-context
                                  d))}}
     :trade-pattern
     {:commands {:save!   (as-> db d
                            (partial trade-pattern-repo/save! d)
                            (partial trade-pattern-svc/save! d
                                     fn-get-account))
                 :delete! (as-> db d
                            (partial trade-pattern-repo/delete! d)
                            (svc.common/delete-fn
                             {:fn-repo-delete!       d
                              :model-type            :trade-pattern
                              :fn-get-referenced-ids (trade-pattern-svc/get-trade-pattern-ids-with-positions-fn
                                                      #(trade-pattern-repo/get-trade-patterns-with-positions db %))}))}
      :queries  {:getn (as-> db d
                         (partial trade-pattern-repo/getn d)
                         (partial trade-pattern-svc/getn d
                                  fn-get-account))
                 :get1 (as-> db d
                         (partial trade-pattern-repo/get1 d)
                         (partial trade-pattern-svc/get1 d))}}
     :holding
     {:commands {:save-holding!     (as-> db d
                                      (partial holding-repo/save-holding! d)
                                      (partial holding-svc/save-holding! d
                                               fn-get-account
                                               common/get-context))
                 ;; TODO: In future positions should be saved together with the holding
                 ;; (added & removed from a holding) & not as a separate document,
                 ;; because Holding is the root aggregate
                 :save-position!    (as-> db d
                                      (partial holding-repo/save-position! d)
                                      (partial holding-svc/save-position! d
                                               fn-get-account
                                               common/get-context))
                 :delete-holdings!  (as-> db d
                                      (partial holding-repo/delete-holdings! d)
                                      (svc.common/delete-msg-fn
                                       {:fn-repo-delete!       d
                                        :model-type            :instrument
                                        :fn-get-ctx            common/get-context
                                        :response-msg-event    :holding.command/delete-holdings!-result
                                        :fn-get-referenced-ids (holding-svc/get-holding-ids-with-positions-fn
                                                                #(holding-repo/get-holdings-with-positions db %))}))
                 :delete-positions! (as-> db d
                                      (partial holding-repo/delete-positions! d)
                                      (svc.common/delete-msg-fn
                                       {:fn-repo-delete!    d
                                        :model-type         :position
                                        :fn-get-ctx         common/get-context
                                        :response-msg-event :holding.command/delete-positions!-result}))}
      :queries  {:get-holdings           (as-> db d
                                           (partial holding-repo/get-holdings d)
                                           (partial holding-svc/get-holdings d
                                                    fn-get-account
                                                    common/get-context))
                 :get-holding            (as-> db d
                                           (partial holding-repo/get-holding d)
                                           (partial holding-svc/get-holding d
                                                    common/get-context))
                 :get-holdings-positions (as-> db d
                                           (partial holding-repo/get-holdings-positions d)
                                           (partial holding-svc/get-holdings-positions d
                                                    #(account-repo/get-by-user-id db %)
                                                    eod/quote-live-prices
                                                    true
                                                    {:fn-get-ctx common/get-context}))
                 :get-holding-positions  (holding-svc/get-holding-positions-fn
                                          #(holding-repo/get-holding-positions db %)
                                          #(account-repo/get-by-user-id db %)
                                          eod/quote-live-prices
                                          common/get-context)

                 :broadcast-holdings-positions (as-> db d
                                                 (partial holding-repo/get-holdings-positions d)
                                                 (partial holding-svc/get-holdings-positions d
                                                          #(account-repo/get-by-user-id db %)
                                                          eod/quote-live-prices
                                                          false)
                                                 (partial holding-svc/broadcast-holdings-positions d))}}
     :model
     {:queries {:getn #(model-svc/getn common/get-context %)}}}))

(defmethod ig/init-key ::handlers
  [_ {:keys [aggregates]}]
  (pprint/pprint {::aggregates aggregates})
  (handlers aggregates))

(defmethod ig/init-key ::broadcasters
  [_ {:keys [aggregates]}]
  {:broadcast-holdings-positions {:fn-start (get-in aggregates [:holding :queries :broadcast-holdings-positions])
                                  :fn-stop  holding-svc/stop-broadcast-holdings-positions}})

(defmethod ig/init-key ::server
  [_ {:keys [aggregates] :as conf}]
  (-> conf
      (assoc :account-svc {:fn-get-account   (get-in aggregates [:account :queries :get1])
                           :fn-save-account! (get-in aggregates [:account :commands :save-any!])})
      server/start!))

(defmethod ig/halt-key! ::server [_ _]
  (server/stop!))

(defmethod ig/init-key ::instrumentation [_ {:keys [nss]}]
  (doall
   (for [n nss]
     (malli.instr/collect! {:ns (the-ns n)})))
  (malli.instr/instrument!))

(defmethod ig/halt-key! ::instrumentation [_ _]
  (malli.instr/unstrument!))

(def sys-def
  {::db              {:fn-authorize auth/authorize
                      :fn-get-ctx   common/get-context}
   ::account-svc     {:db (ig/ref ::db)}
   ::aggregates      {:db (ig/ref ::db)}
   ::handlers        {:aggregates (ig/ref ::aggregates)}
   ::broadcasters    {:aggregates (ig/ref ::aggregates)}
   ::server          {:handlers     (ig/ref ::handlers)
                      :aggregates   (ig/ref ::aggregates)
                      :port         5000
                      :broadcasters (ig/ref ::broadcasters)}
   ::instrumentation {:nss ['reason-alpha.data.model
                            'reason-alpha.data.repositories.account-repository
                            'reason-alpha.data.repositories.holding-repository
                            'reason-alpha.data.repositories.trade-pattern-repository
                            'reason-alpha.infrastructure.auth
                            'reason-alpha.model.common
                            'reason-alpha.services.holding-service]}})

(def *system
  (atom nil))

(defn start-system []
  (reset! *system (ig/init sys-def)))

(defn stop-system! []
  (ig/halt! @*system))

(comment
  @*system

  (let [db (get-in @*system [:reason-alpha.model/db])]
    (holding-repo/get-holdings-positions db {:account-id #uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49"}))
  )
