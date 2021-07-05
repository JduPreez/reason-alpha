(ns reason-alpha.web.routes
  (:require [reitit.ring :as ring]))

(defn- default-handler [_]
  {:status 200})

(defn app-router
  ([method-handlers routes-data]
   (ring/router
    [["/api"
      ["/trade-patterns" {:name :trade-patterns/*
                                        ;:parameters {:path {:user-id uuid?}}
                          :get  (or (get-in method-handlers [:trade-pattern/* :get])
                                    default-handler)
                          :post (or (get-in method-handlers [:trade-pattern/* :post])
                                    default-handler)}]
      ["/trade-patterns/:id" {:name :trade-patterns
                                        ;:parameters {:path {:id      uuid?}}
                              :put  (or (get-in method-handlers [:trade-pattern :put])
                                        default-handler)}]
      ["/ping" {:name :ping
                :get  (or (get-in method-handlers [:ping :get])
                          default-handler)}]]]
    {:data routes-data}))
  ([]
   (app-router nil nil)))
