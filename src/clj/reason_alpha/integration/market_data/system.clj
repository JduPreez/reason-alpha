(ns reason-alpha.integration.market-data.system
  (:require [clojure.core.async :as as]
            [integrant.core :as ig]
            [outpace.config :refer [defconfig]]
            [reason-alpha.data.model :as data.model :refer [DataBase]]
            [reason-alpha.data.xtdb :as xtdb]
            [reason-alpha.infrastructure.auth :as auth]
            [reason-alpha.infrastructure.message-processing :as msg-processing]
            [reason-alpha.model.common :as common]
            [reason-alpha.model.utils :as mutils]))

(defconfig db-conf)

(defmethod ig/init-key ::db [_ {:keys [fn-authorize fn-get-ctx]}]
  (let [{:keys [data-dir db-name]} db-conf
        db                         (xtdb/db :fn-get-ctx fn-get-ctx
                                            :fn-authorize fn-authorize
                                            :data-dir data-dir
                                            :db-name db-name)]
    (data.model/connect db)
    {:db-instance db
     :data-dir    data-dir
     :db-name     db-name}))

(defmethod ig/init-key ::aggregates [_ {{db :db-instance} :db}]
  {}
  #_{:equity
   {:queries {:getn (as-> db d
                      (partial ))}}})

(defmethod ig/init-key ::handlers
  [_ {:keys [aggregates]}]
  (clojure.pprint/pprint {::aggregates aggregates})
  (mutils/handlers aggregates))

(defn sys-def []
  {::db         {:fn-authorize auth/authorize
                 :fn-get-ctx   common/get-context}
   ::aggregates {:db (ig/ref ::db)}
   ::handlers   {:aggregates (ig/ref ::aggregates)}})

(def *system (atom nil))

(defn start-system []
  (reset! *system (ig/init (sys-def))))

(defn stop-system! []
  (ig/halt! @*system))

(defn init-module-service []
  (let [c (msg-processing/receive-msg :module-service/switch)]
    (as/go-loop []
      (println ::init-module-service :running)
      (let [{state :msg/value} (as/<! c)]
        (println ::init-module-service "received switch message" state)
        (case state
          :module-service.state/start (start-system)
          (stop-system!))
        (recur)))))

(init-module-service)

(comment

  (start-system)

  (init-module-service)

  (msg-processing/send-msg {:msg/type  :module-service/switch
                            :msg/value :module-service.state/start})

  )
