(ns reason-alpha.web.routes.home
  (:require [reason-alpha.web.services :as svc]
            [reason-alpha.web.layout :as layout]
            [reason-alpha.web.middleware :as middleware]))

(defn home-page [request]
  (layout/render request "reason-alpha.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}
    "/api/trade-patterns" {:get  svc/get-trade-patterns
                           :post svc/save-trade-pattern!}
    "/api/trade-patterns/:id" {:get     {:parameters {:path-params ::request}}
                               :handler svc/get-trade-pattern
                               :put     svc/save-trade-pattern!}]])