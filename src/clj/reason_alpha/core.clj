(ns reason-alpha.core
  (:require [mount.lite :refer (defstate) :as mount]
            [reason-alpha.web.handler :as handler]
            [ring.adapter.jetty :as jetty]))

(defonce server (jetty/run-jetty (handler/app) {:port  3000
                                                :join? false}))

(defstate app
  :start (do
           (.start server)
           server)
  :stop  (.stop server))

(defn -main []
  (mount/start))

