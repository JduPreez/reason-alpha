(ns reason-alpha.web.routes
  (:require [reitit.ring :as ring]))

(defn- default-handler [_]
  {:status 200})

(defn app-router
  ([method-handlers routes-data]
   (ring/router
    [["/api"
      ["" {:name :api
           :get  (or (get-in method-handlers [:api :get])
                     default-handler)}]
      ["/trade-patterns" {:name :trade-patterns
                                        ;:parameters {:path {:user-id uuid?}}
                          :get  (or (get-in method-handlers [:trade-patterns :get])
                                    default-handler)
                          :post (or (get-in method-handlers [:trade-patterns :post])
                                    default-handler)}]
      ["/trade-patterns/:id" {:name   :trade-patterns/id
                                        ;:parameters {:path {:id      uuid?}}
                              :put    (or (get-in method-handlers [:trade-patterns/id :put])
                                          default-handler)
                              :delete (or (get-in method-handlers [:trade-patterns/id :delete])
                                          default-handler)}]
      ["/ping" {:name :ping
                :get  (or (get-in method-handlers [:ping :get])
                          default-handler)}]]]
    {:data routes-data}))
  ([]
   (app-router nil nil)))
