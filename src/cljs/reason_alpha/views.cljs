(ns reason-alpha.views
  (:require [accountant.core :as accountant]
            [re-frame.core :as rf]
            [reagent.dom :as r.dom]
            [reason-alpha.views.main :as main]
            [reason-alpha.views.trade-patterns :as trade-patterns]
            [reason-alpha.views.trades :as trades]
            [reason-alpha.views.datagrid :as datagrid]
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
   ["" {:name  ::trades/view
        :view  trades/view
        :model :trade
        :controllers
        [{:start (fn [& params] (js/console.log "Entering :trades"))
          :stop  (fn [& params] (js/console.log "Leaving :trades"))}]}]
   ["trade-patterns" {:name              ::trade-patterns/view
                      :view              trade-patterns/view
                      :model             :trade-pattern
                      :data-subscription :trade-pattern.query/get
                      :controllers
                      [{:start (fn [& params] (cljs.pprint/pprint ["Entering :trade-patterns" params]))
                        :stop  (fn [& params] (js/console.log "Leaving :trade-patterns"))}]}]])

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

#_(defn navigate [view]
  [:navigate (reitit/match-by-name router view)])

(defn init []
  (init-routes!)
  (r.dom/render [router-component {:router router}]
                  (.getElementById js/document "app")))
