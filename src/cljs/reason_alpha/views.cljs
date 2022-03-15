(ns reason-alpha.views
  (:require [accountant.core :as accountant]
            [re-frame.core :as rf]
            [reagent.dom :as r.dom]
            [reason-alpha.views.datagrid :as datagrid]
            [reason-alpha.views.instruments :as instruments]
            [reason-alpha.views.main :as main]
            [reason-alpha.views.positions :as positions]
            [reason-alpha.views.trade-patterns :as trade-patterns]
            [reitit.coercion.spec :as rss]
            [reitit.core :as r]
            [reitit.frontend :as rfe]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfeasy]))

;;; Routes ;;;

(defn href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (href k nil nil))
  ([k params]
   (href k params nil))
  ([k params query]
   (rfeasy/href k params query)))

(def routes
  ["/"
   ["" {:name    ::positions/view
        :view    positions/view
        :model   :position
        :load-fx :position.query/getn
        :controllers
        [{:start (fn [& params] (js/console.log "Entering :holdings"))
          :stop  (fn [& params] (js/console.log "Leaving :holdings"))}]}]
   ["trade-patterns" {:name       ::trade-patterns/view
                      :view       trade-patterns/view
                      :model      :trade-pattern
                      :load-event [:trade-pattern/load]}]
   ["instruments" {:name       ::instruments/view
                   :view       instruments/view
                   :model      :instrument
                   :load-event [:instrument/load]}]])

(defn on-navigate [new-match]
  (when new-match
    (rf/dispatch [:navigated new-match])))

(def router
  (rfe/router
   routes
   {:data {:coercion rss/coercion}}))

(defn init-routes! []
  (js/console.log "initializing routes")
  (rfeasy/start!
   router
   on-navigate
   {:use-fragment true}))

(defn router-component [{:keys [router]}]
  (let [current-route @(rf/subscribe [:current-route])]
    (when current-route
      [main/view (-> current-route :data :view)])))

(defn init []
  (init-routes!)
  (r.dom/render [router-component {:router router}]
                  (.getElementById js/document "app")))
