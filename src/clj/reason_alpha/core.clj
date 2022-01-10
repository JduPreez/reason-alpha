(ns reason-alpha.core
  (:require [malli.instrument :as malli.instr]
            [mount.lite :refer (defstate) :as mount]
            [reason-alpha.server :as server]
            [reason-alpha.services.trade-pattern]
            ;;[reason-alpha.web.handler :as handler]
            #_[ring.adapter.jetty :as jetty]))

#_(defonce server (delay
                  (jetty/run-jetty (handler/app) {:port  3000
                                                  :join? false})))

#_(defstate app
   :start (do
            (.start @server)
            server)
   :stop  (.stop @server))

#_(defstate app
  :start (server/start!)
  :stop (server/stop!))

(defn -main []
  (mount/start)
  (server/start!)
  (malli.instr/instrument!))


