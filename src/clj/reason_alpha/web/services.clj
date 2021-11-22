(ns reason-alpha.web.services
  (:require  [reason-alpha.data :as data :refer [any save! db delete!]]
             [reason-alpha.services.trades :as trades-svc]
             [ring.util.http-response :refer :all]))

; TODO: Improve error handling

(defn get-trade-patterns [_] #_[{{{user-id :user-id} :path} :parameters}]  
  (ok {:result (trades-svc/get-trade-patterns)})) ;; (choose db [:trade-pattern/*])

(defn get-trade-pattern [{{{:keys [id]} :path} :parameters}]
  (ok {:result (any db [:trade-pattern/id := id])}))

(defn save-trade-pattern! [{trade-pattern :body-params}]
  (ok {:result (trades-svc/save-trade-pattern! trade-pattern)}))

(defn delete-trade-patterns! [{{{:keys [id]} :path} :parameters}]
  (ok {:result (trades-svc/delete-trade-patterns! id)}))

(defn get-api-info [_]
  (ok {:result "Welcome to the Reason Alpha API :-)"}))

(comment
  (delete! db [:trade-pattern/id := #uuid "017ade64-1bfa-1586-119b-87df8dbec35d"])

  (trades-svc/get-trade-patterns)

  (select db [:trade-pattern/*])
  (save! db {:trade-pattern/name        "Buy Support or Short Resistance",
             :trade-pattern/creation-id nil,
             :trade-pattern/id          #uuid "01738610-a026-1f53-5d94-219803fa47e1",
             :trade-pattern/parent-id   #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6",
             :trade-pattern/user-id     #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f",
             :trade-pattern/description "another test"})

  (trades-svc/save-trade-pattern! {:trade-pattern/name           "test 3",
                                   :trade-pattern/creation-id    #uuid "01738610-a026-1f53-5d94-219803fa47e1",
                                   :trade-pattern/parent-id      #uuid "017a2a48-4694-dfcc-3e52-98e5ac8c74db",
                                   :trade-pattern/description    ""
                                   :trade-pattern/ancestors-path ["Breakout", "-"]})
  )
