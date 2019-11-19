(ns reason-alpha.web.handler
  (:require
   [reason-alpha.web.middleware :as middleware]
   [reason-alpha.web.layout :refer [error-page]]
   [reason-alpha.web.routes.home :refer [home-routes]]
   [reitit.ring :as reitit]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.webjars :refer [wrap-webjars]]
   #_[reason-alpha.env :refer [defaults]]
   #_[mount.core :as mount]))

#_(mount/defstate init-app
  :start ((or (:init defaults) (fn [])))
  :stop  ((or (:stop defaults) (fn []))))

(def app-routes
  (reitit/ring-handler
   (reitit/router
    [(home-routes)])
   (reitit/routes
    (reitit/create-resource-handler
     {:path "/"})
    (wrap-content-type
     (wrap-webjars (constantly nil)))
    (reitit/create-default-handler
     {:not-found          (constantly (error-page {:status 404
                                                   :title  "404 - Page not found"}))
      :method-not-allowed (constantly (error-page {:status 405
                                                   :title  "405 - Not allowed"}))
      :not-acceptable     (constantly (error-page {:status 406
                                                   :title  "406 - Not acceptable"}))}))))

(defn app []
  (middleware/wrap-base #'app-routes))
