(ns reason-alpha.core
  (:require [reason-alpha.web.handler :as handler]
            [ring.adapter.jetty :as jetty]))


#_(defn handler [request-map]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (str "<html><body> your IP is: "
                 (:remote-addr request-map)
                 "</body></html>")})

(defn -main []
  (jetty/run-jetty
   (handler/app)
   {:port  3000
    :join? false}))