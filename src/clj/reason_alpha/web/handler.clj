(ns reason-alpha.web.handler
  (:require [clojure.spec.alpha :as s]
            [reason-alpha.web.middleware :as middleware]
            [reason-alpha.web.middleware.exception :as exception]
            [reason-alpha.web.middleware.formats :as formats]
            [reason-alpha.web.routes :as routes]
            [reason-alpha.web.services :as svc]
            [reitit.ring :as ring]
            [reitit.coercion :as coercion]
            [reitit.coercion.spec :as spec-coercion]
            [reitit.ring.coercion :as ring-coercion]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.util.http-response :refer :all]))

(def router (routes/app-router {:trade-pattern {:get svc/get-trade-patterns
                                                :put svc/save-trade-pattern!}
                                :ping          {:get (constantly (ok {:message "pong"}))}}
                               {:coercion   spec-coercion/coercion
                                :muuntaja   formats/instance
                                :middleware [parameters/parameters-middleware
                                             muuntaja/format-negotiate-middleware
                                             muuntaja/format-response-middleware
                                             exception/exception-middleware
                                             muuntaja/format-request-middleware
                                             ring-coercion/coerce-request-middleware
                                             ring-coercion/coerce-response-middleware
                                             multipart/multipart-middleware]}))

(def app-routes
  (ring/ring-handler
   router
   (ring/create-default-handler)))

(defn app []
  (middleware/wrap-base #'app-routes))

(comment
  (require '[reitit.core :as r])
  
  (coercion/coerce! 
   (r/match-by-path  router "/api/users/8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f/trade-patterns"))

   (r/match-by-name :trade-pattern)
  
  (remove-ns 'reason-alpha.web.handler))
