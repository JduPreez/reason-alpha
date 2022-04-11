(ns reason-alpha.model
  (:require [clojure.pprint :as pprint]
            [integrant.core :as ig]
            [malli.instrument :as malli.instr]
            [reason-alpha.data.model :as data.model :refer [DataBase]]
            [reason-alpha.data.repositories.account-repository :as account-repo]
            [reason-alpha.data.repositories.holding-repository :as holding-repo]
            [reason-alpha.data.repositories.position-repository :as position-repo]
            [reason-alpha.data.repositories.trade-pattern-repository :as trade-pattern-repo]
            [reason-alpha.data.xtdb :as xtdb]
            [reason-alpha.infrastructure.auth :as auth]
            [reason-alpha.infrastructure.server :as server]
            [reason-alpha.model.accounts :as accounts]
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

(defmethod ig/halt-key! ::db [_ _]
  (data.model/disconnect xtdb/db))

(defmethod ig/init-key ::aggregates [_ {db                       :db
                                        {:keys [fn-get-account]} :account-svc}]
  {:trade-pattern
   {:commands {:save!   (as-> db d
                          (partial trade-pattern-repo/save! d)
                          (partial trade-pattern-svc/save! d
                                   fn-get-account))
               :delete! (as-> db d
                          (partial trade-pattern-repo/delete! d)
                          (svc.common/delete-fn
                           d
                           :trade-pattern))}
    :queries  {:getn (as-> db d
                       (partial trade-pattern-repo/getn d)
                       (partial trade-pattern-svc/getn d
                                fn-get-account))
               :get1 (as-> db d
                       (partial trade-pattern-repo/get1 d)
                       (partial trade-pattern-svc/get1 d))}}
   :position
   {:queries {:get-positions (as-> db d
                               (partial position-repo/getn d)
                               (svc.common/getn-msg-fn
                                {:fn-repo-getn       d
                                 :fn-get-account     fn-get-account
                                 :fn-get-ctx         common/get-context
                                 :response-msg-event :position.query/get-position-result}))
              :get-position  (as-> db d
                               (partial position-repo/get1 d)
                               (svc.common/get1-msg-fn
                                {:fn-repo-get1       d
                                 :fn-get-ctx         common/get-context
                                 :response-msg-event :position.query/get-position-result}))}}
   ;; TODO: In future positions should be saved together with the holding & not as a separate document,
   ;; because Holding is the root aggregate
   :holding
   {:commands {:save-holding!    (as-> db d
                                   (partial holding-repo/save! d)
                                   (partial holding-svc/save-holding! d
                                            fn-get-account
                                            common/get-context))
               :save-position!   (as-> db d
                                   (partial position-repo/save! d)
                                   (svc.common/save-msg-fn
                                    {:model-type         :position
                                     :fn-repo-save!      d
                                     :fn-get-ctx         common/get-context
                                     :fn-get-account     fn-get-account
                                     :response-msg-event :holding.command/save-position!-result}))
               :delete-holding!  (as-> db d
                                   (partial holding-repo/delete! d)
                                   (svc.common/delete-msg-fn
                                    {:fn-repo-delete!    d
                                     :model-type         :instrument
                                     :fn-get-ctx         common/get-context
                                     :response-msg-event :holding.command/delete-holding!-result}))
               :delete-position! (as-> db d
                                   (partial position-repo/delete! d)
                                   (svc.common/delete-msg-fn
                                    {:fn-repo-delete!    d
                                     :model-type         :position
                                     :fn-get-ctx         common/get-context
                                     :response-msg-event :holding.command/delete-position!-result}))}
    :queries  {:get-holdings (as-> db d
                               (partial holding-repo/getn d)
                               (partial holding-svc/get-holdings d
                                        fn-get-account
                                        common/get-context))
               :get-holding  (as-> db d
                               (partial holding-repo/get1 d)
                               (partial holding-svc/get-holding d
                                        common/get-context))}}
   :model
   {:queries {:getn #(model-svc/getn common/get-context %)}}})

(defmethod ig/init-key ::handlers [_ {:keys [aggregates]}]
  (pprint/pprint {::aggregates aggregates})
  (handlers aggregates))

(defmethod ig/init-key ::server [_ conf]
  (server/start! conf))

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
   ::aggregates      {:db          (ig/ref ::db)
                      :account-svc (ig/ref ::account-svc)}
   ::handlers        {:aggregates (ig/ref ::aggregates)}
   ::server          {:handlers    (ig/ref ::handlers)
                      :account-svc (ig/ref ::account-svc)
                      :port        5000}
   ::instrumentation {:nss ['reason-alpha.data.model
                            'reason-alpha.data.repositories.account-repository
                            'reason-alpha.data.repositories.holding-repository
                            'reason-alpha.data.repositories.position-repository
                            'reason-alpha.data.repositories.trade-pattern-repository
                            'reason-alpha.infrastructure.auth
                            'reason-alpha.services.holding-service]}})

(def *system
  (atom nil))

(defn start-system []
  (reset! *system (ig/init sys-def)))

(defn stop-system! []
  (ig/halt! @*system))
