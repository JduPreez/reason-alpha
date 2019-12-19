(ns reason-alpha.web.services
  (:require  [reason-alpha.data :refer [choose any save! db]]
             [ring.util.http-response :as response]))

; TODO: Improve error handling

(defn get-trade-patterns [{{:keys [user-id]} :path-params}]
  (response/ok (choose db [:trade-pattern/user-id = user-id])))

(defn get-trade-pattern [{{:keys [id]} :path-params}]
  (response/ok (any db [:trade-pattern/id = id])))

(defn save-trade-pattern! [req]
  (let [trade-pattern (:params req)]
    (response/ok (save! db trade-pattern))))