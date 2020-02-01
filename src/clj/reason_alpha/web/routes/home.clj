(ns reason-alpha.web.routes.home
  (:require [clojure.tools.logging :as log]
            [reason-alpha.web.services :as svc]
            [reason-alpha.web.layout :as layout]
            [reason-alpha.web.middleware :as middleware]
            [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "reason-alpha.html"))

(defn home-routes []
  [""
   #_{:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   {:middleware [middleware/wrap-formats]}
   ["/" {:get home-page}
    "/api/trade-patterns" {:get  (response/ok {:status "success"})
                           :post svc/save-trade-pattern!}
    "/api/trade-patterns/:id" {:get     {:parameters {:path-params ::request}}
                               :handler svc/get-trade-pattern
                               :put     svc/save-trade-pattern!}]])