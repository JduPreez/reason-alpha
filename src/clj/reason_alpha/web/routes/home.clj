(ns reason-alpha.web.routes.home
  (:require [reason-alpha.web.layout :as layout]           
            [reason-alpha.web.middleware :as middleware]))

(defn home-page [request]
  (layout/render request "reason-alpha.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]])