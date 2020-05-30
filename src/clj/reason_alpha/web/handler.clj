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

#_(def routes-defs
  ["/api"
   ["/users/:user-id/trade-patterns" {:name       :trade-pattern/get
                                      :parameters {:path {:user-id uuid?}}
                                      :get        svc/get-trade-patterns}]

   ["/ping" {:name :ping
             :get (constantly (ok {:message "pong"}))}]])

#_(def route-defs
  (routes/defs {:trade-pattern {:get svc/get-trade-patterns}
                :ping          {:get (constantly (ok {:message "pong"}))}}))

#_(def router (ring/router
             [route-defs]
             {;:compile coercion/compile-request-coercers
              :data    {:coercion   spec-coercion/coercion
                        :muuntaja   formats/instance
                        :middleware [parameters/parameters-middleware
                                     muuntaja/format-negotiate-middleware
                                     muuntaja/format-response-middleware
                                     exception/exception-middleware
                                     muuntaja/format-request-middleware
                                     ring-coercion/coerce-request-middleware
                                     ring-coercion/coerce-response-middleware
                                     multipart/multipart-middleware]}}))
(def router (routes/app-router {:trade-pattern {:get svc/get-trade-patterns}
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
  
  (remove-ns 'reason-alpha.web.handler))
