(ns reason-alpha.views
  (:require [re-frame.core :as rf]
            [reagent.dom :as r.dom]
            [reason-alpha.views.datagrid :as datagrid]
            [reason-alpha.views.holdings :as holdings]
            [reason-alpha.views.main :as main]
            [reason-alpha.views.positions :as positions]
            [reason-alpha.views.trade-patterns :as trade-patterns]
            [reason-alpha.views.accounts :as accounts]
            [reitit.coercion.spec :as rss]
            [reitit.core :as r]
            [reitit.frontend :as rfe]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe-easy]
            [schema.core :as s]))

(declare router)

(def routes
  ["/"
   ["" {:name       ::positions/view
        :view       positions/view
        :model      :position
        :load-event [:position/load]
        :controllers
        [{:parameters {:query [:id]}
          :start      (fn [& params]
                        (cljs.pprint/pprint ["Entering :positions" params]))
          :stop       (fn [& params] (js/console.log "Leaving :positions"))}]}]
   ["trade-patterns" {:name       ::trade-patterns/view
                      :view       trade-patterns/view
                      :model      :trade-pattern
                      :load-event [:trade-pattern/load]}]
   ["holdings" {:name       ::holdings/view
                :view       holdings/view
                :model      :holding
                :load-event [:holding/load]}]
   ["forms"
    ["/account-profile" {:name       ::accounts/form-view
                         :view       accounts/view
                         :model      :account
                         :load-event [:account/load]}]]])

(defn on-navigate [new-match]
  (when new-match
    (rf/dispatch [:navigated new-match router])))

(def router
  (rfe/router
   routes
   {:data {:coercion rss/coercion}}))

(comment
  (r/match-by-path router "/forms/account-profile")
  
  (-> (r/match-by-name router ::accounts/form-view)
      r/match->path)
  (r/routes router)

  (let [url (-> js/window .-location js/URL.)]
    (-> url .-hash .toString)
    #_(-> url .-searchParams (.set "form" "account-profile"))
    #_(-> url .toString))

  (rfe-easy/href ::holdings/view nil {:form "account-profile"})

  (let [{{frm-name  :name
          frm-model :model} :data} nil]
    frm-name)

  )

(defn init-routes! []
  (js/console.log "initializing routes")
  (rfe-easy/start!
   router
   on-navigate
   {:use-fragment true}))

(defn router-component [{:keys [router]}]
  (let [{{sv :view} :sheet-view
         {fv :view} :form-view} @(rf/subscribe [:active-view-model])]
    (when sv
      [main/view
       :sheet-view sv
       :form-view fv
       :accounts-form-view ::accounts/form-view
       :positions-sheet-view ::positions/view])))

(defn init []
  (init-routes!)
  (r.dom/render [router-component {:router router}]
                  (.getElementById js/document "app")))
