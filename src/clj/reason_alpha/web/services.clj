(ns reason-alpha.web.services
  (:require  [reason-alpha.data :refer [choose any save! db]]
             [ring.util.http-response :refer :all]))

; TODO: Improve error handling

(defn get-trade-patterns [{{{user-id :user-id} :path} :parameters}]  
  (ok {:result (choose db [:trade-pattern/user-id := user-id]) }))

(defn get-trade-pattern [{{:keys [id]} :path-params}]
  (ok (any db [:trade-pattern/id = id])))

(defn save-trade-pattern! [req]
  (let [trade-pattern (:params req)]
    (ok (save! db trade-pattern))))

(comment
  (choose db [:trade-pattern/user-id := #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"]))