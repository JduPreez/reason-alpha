(ns reason-alpha.web.services
  (:require  [reason-alpha.data :refer [choose any save! db]]
             [reason-alpha.services.trades :as trades-svc]
             [ring.util.http-response :refer :all]))

; TODO: Improve error handling

(defn get-trade-patterns [_] #_[{{{user-id :user-id} :path} :parameters}]  
  (ok {:result (trades-svc/get-trade-patterns)})) ;; (choose db [:trade-pattern/*])

(defn get-trade-pattern [{{:keys [id]} :path-params}]
  (ok (any db [:trade-pattern/id = id])))

(defn save-trade-pattern! [{trade-pattern :body-params}]
  (ok {:result (trades-svc/save-trade-pattern! trade-pattern)}))

(comment
  (choose db [:trade-pattern/*])
  (save! db {:trade-pattern/name "Buy Support or Short Resistance",
             :trade-pattern/creation-id nil,
             :trade-pattern/id #uuid "01738610-a026-1f53-5d94-219803fa47e1",
             :trade-pattern/parent-id
             #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6",
             :trade-pattern/user-id #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f",
             :trade-pattern/description "another test"})
  )
