(ns reason-alpha.core
  (:require [malli.instrument :as malli.instr]
            [mount.lite :refer (defstate) :as mount]
            [reason-alpha.web.server :as web-server]
            #_[reason-alpha.web.handler :as handler]
            #_[ring.adapter.jetty :as jetty]))

#_(defonce server (delay
                  (jetty/run-jetty (handler/app) {:port  3000
                                                  :join? false})))

#_(defstate app
   :start (do
            (.start @server)
            server)
   :stop  (.stop @server))

(defstate app
  :start (web-server/start!)
  :stop (web-server/stop!))

(defn -main []
  (mount/start)
  (malli.instr/instrument!))


