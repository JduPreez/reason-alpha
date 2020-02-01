(ns reason-alpha.core
  (:require #_[mount.lite :refer (defstate) :as mount]
            [reason-alpha.web.handler :as handler]
            [ring.adapter.jetty :as jetty]))

#_(defonce server (jetty/run-jetty #'handler/app {:port  3000
                                                :join? false}))

#_(defstate app
  :start (do
           (.start server)
           (println "Reason Alpha is running on http://localhost:3000"))
  :stop  (.stop server))

(defn -main []
  (jetty/run-jetty #'handler/app-routes {:port  3000
                                         :join? false})
  #_(mount/start))