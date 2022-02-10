(ns reason-alpha.model
  (:require [clojure.pprint :as pprint]
            [integrant.core :as ig]
            [reason-alpha.data.crux :as crux]
            [reason-alpha.data.model :as data.model :refer [DataBase]]
            [reason-alpha.data.repositories.trade-pattern :as repo.trade-pattern]
            [reason-alpha.infrastructure.server]
            [reason-alpha.services.trade-pattern :as svc.trade-pattern]
            [traversy.lens :as tl]
            [reason-alpha.infrastructure.server :as server]))

;; TODO: Route queries directly to repo functions.
#_(def aggregates
  {:trade-pattern
   {:commands {:save!   svc.trade-pattern/save!
               :delete! svc.trade-pattern/delete!}
    :queries  {:get  svc.trade-pattern/getn
               :get1 svc.trade-pattern/get1}} ;; TODO: Fix get-trade-pattern input args to handle command
   :position
   {:commands {:save! nil}
    :queries  {:get  nil
               :get1 nil}}})

(defn handlers [aggregates]
  (-> aggregates
      (tl/update
       tl/all-entries
       (fn [[aggr-k {cmds  :commands
                     qries :queries}]]
         (letfn [(to-ns-keys [{:keys [commands queries]}]
                   (-> commands
                       (or queries)
                       (tl/update
                        tl/all-keys
                        #(-> aggr-k
                             name
                             (str "." (if commands "command" "query") "/" (name %))
                             keyword))))]
           (merge (to-ns-keys {:commands cmds})
                  (to-ns-keys {:queries qries})))))))

(defmethod ig/init-key ::db [_ _opts]
  (data.model/connect crux/db)
  crux/db)

(defmethod ig/halt-key! ::db [_ _]
  (data.model/disconnect crux/db))

(defmethod ig/init-key ::aggregates [_ {:keys [db]}]
  {:trade-pattern
   {:commands {:save! (as-> db d
                        (partial repo.trade-pattern/save! d)
                        (partial svc.trade-pattern/save! d))}
    ;; TODO: Change `:get` to `:getn`
    :queries  {:get  (as-> db d
                       (partial repo.trade-pattern/getn d)
                       (partial svc.trade-pattern/getn d))
               :get1 svc.trade-pattern/get1}}})

(defmethod ig/init-key ::handlers [_ {:keys [aggregates]}]
  (pprint/pprint aggregates)
  (handlers aggregates))

(defmethod ig/init-key ::server [_ {:keys [handlers]}]
  (server/start! handlers))

(defmethod ig/halt-key! ::server [_ _]
  (server/stop!))

(def sys-def
  {::db         {}
   ::aggregates {:db (ig/ref ::db)}
   ::handlers   {:aggregates (ig/ref ::aggregates)}
   ::server     {:handlers (ig/ref ::handlers)}})

(def system
  (ig/init sys-def))

(defn stop-system! []
  (ig/halt! system))

(comment
  (let [m {:trade-pattern
           {:commands {:save! nil},
            :queries
            {:get  nil,
             :get1 nil}}}]
    (handlers m))

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
