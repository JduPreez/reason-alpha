(ns reason-alpha.model
  (:require [clojure.pprint :as pprint]
            [integrant.core :as ig]
            [malli.instrument :as malli.instr]
            [reason-alpha.data.model :as data.model :refer [DataBase]]
            [reason-alpha.data.repositories.account :as repo.account]
            [reason-alpha.data.repositories.instrument :as repo.instrument]
            [reason-alpha.data.repositories.position :as repo.position]
            [reason-alpha.data.repositories.trade-pattern :as repo.trade-pattern]
            [reason-alpha.data.xtdb :as xtdb]
            [reason-alpha.infrastructure.auth :as auth]
            [reason-alpha.infrastructure.server :as server]
            [reason-alpha.model.accounts :as accounts]
            [reason-alpha.model.common :as common]
            [reason-alpha.services.account :as svc.account]
            [reason-alpha.services.instrument :as svc.instrument]
            [reason-alpha.services.model :as svc.model]
            [reason-alpha.services.position :as svc.position]
            [reason-alpha.services.trade-pattern :as svc.trade-pattern]
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

(defmethod ig/init-key ::db [_ _opts]
  (data.model/connect xtdb/db)
  xtdb/db)

(defmethod ig/init-key ::account-svc [_ {:keys [db]}]
  (let [fn-repo-save!      #(repo.account/save! db %)
        fn-repo-get-by-uid #(repo.account/get-by-user-id db %)]
    {:fn-get-account   #(svc.account/get-account common/get-context
                                                 fn-repo-get-by-uid %)
     :fn-save-account! #(svc.account/save! fn-repo-get-by-uid
                                           fn-repo-save! %)}))

(defmethod ig/halt-key! ::db [_ _]
  (data.model/disconnect xtdb/db))


;; https://stackoverflow.com/questions/26116277/getting-argument-type-hints-of-a-function-in-clojure
;; Tag context arg un function & inject from the server messaging
(defmethod ig/init-key ::aggregates [_ {db                       :db
                                        {:keys [fn-get-account]} :account-svc}]
  {:trade-pattern
   {:commands {:save!   (as-> db d
                          (partial repo.trade-pattern/save! d)
                          (partial svc.trade-pattern/save! d
                                   fn-get-account))
               :delete! (as-> db d
                          (partial repo.trade-pattern/delete! d)
                          (partial svc.trade-pattern/delete! d
                                   fn-get-account))}
    :queries  {:getn (as-> db d
                       (partial repo.trade-pattern/getn d)
                       (partial svc.trade-pattern/getn d
                                fn-get-account))
               :get1 (as-> db d
                       (partial repo.trade-pattern/get1 d)
                       (partial svc.trade-pattern/get1 d
                                fn-get-account))}}
   :position
   {:commands {:save! (as-> db d
                        (partial repo.position/save! d)
                        (partial svc.position/save! d fn-get-account))}
    :queries  {:getn (as-> db d
                       (partial repo.position/getn d))}}

   :instrument
   {:commands {:save! (as-> db d
                        (partial repo.instrument/save! d)
                        (partial svc.instrument/save! d
                                 fn-get-account
                                 common/get-context))}
    :queries  {:getn (as-> db d
                       (partial repo.instrument/getn d)
                       (partial svc.instrument/getn d
                                fn-get-account
                                common/get-context))
               :get1 (as-> db d
                       (partial repo.instrument/get1 d)
                       (partial svc.instrument/get1 d
                                fn-get-account
                                common/get-context))}}
   :model
   {:queries {:getn #(svc.model/getn common/get-context %)}}})

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
  {::db              nil
   ::account-svc     {:db (ig/ref ::db)}
   ::aggregates      {:db          (ig/ref ::db)
                      :account-svc (ig/ref ::account-svc)}
   ::handlers        {:aggregates (ig/ref ::aggregates)}
   ::server          {:handlers    (ig/ref ::handlers)
                      :account-svc (ig/ref ::account-svc)
                      :port        5000}
   ::instrumentation {:nss ['reason-alpha.data.model
                            'reason-alpha.data.repositories.account
                            'reason-alpha.data.repositories.position
                            'reason-alpha.infrastructure.auth
                            'reason-alpha.services.instrument
                            'reason-alpha.services.position]}})

(def *system
  (atom nil))

(defn start-system []
  (reset! *system (ig/init sys-def)))

(defn stop-system! []
  (ig/halt! @*system))

(comment
  (def m {:some "data"
          :more "data"})

  (let [x  {:some "data"
            :more "data"}
        mx (with-meta x {:bye true})]
    (clojure.pprint/pprint (meta mx)))

  (require '[malli.instrument :as malli.instr])

  (start-system)

  @*system

  (let [aggregates {:trade-pattern
                    {:commands
                     {:save!
                      0},
                     :queries
                     {:getn 1,
                      :get1 2}},
                    :holding
                    {:commands
                     {:save! 3}}}]
    (handlers aggregates))

  (let [m        (ig/init config)
        id       :trade-pattern.query/get
        handlers (:reason-alpha.model/handlers m)
        fun      (get handlers id)]
    (fun nil))

  (-> {:test "test"}
      (partial repo.trade-pattern/save!)
      (partial svc.trade-pattern/save!))

  (let [system ]
    system)


  )
