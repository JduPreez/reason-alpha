(ns reason-alpha.views
  (:require [reitit.core :as reitit]))

(def router
  (reitit/router
   [["/" :trades]
    ["/about" :about]
    ["/trade-patterns" :trade-patterns]]))

(defn navigate [view]
  [:navigate (reitit/match-by-name router view)])
