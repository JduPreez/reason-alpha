(ns reason-alpha.core
  (:require [clojure.string :as string]
            [day8.re-frame.http-fx]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [re-frame.core :as rf]
            [reagent.dom :as r.dom]
            [ra-datagrid.core]
            [reason-alpha.events]
            [reason-alpha.events.trade-patterns]
            [reason-alpha.subs]
            [reason-alpha.views.main :as main]
            [reason-alpha.views.trade-patterns :as trade-patterns]
            [reason-alpha.views.trades :as trades]
            [reitit.core :as reitit])
  (:import goog.History))

;; -------------------------
;; Routes
(def router
  (reitit/router
   [["/" :home]
    ["/about" :about]
    ["/trade-patterns" :trade-patterns]]))

(defn get-header-menu-items [menu-items params]
  (let [col    (.-column params)
        col-id (.-colId col)]
    (if-let [[name route] (get menu-items col-id)]
      (clj->js [{"name"    name
                 :action #(rf/dispatch-sync [:navigate (reitit/match-by-name router route)])}])
      (.-defaultItems col))))

#_(defn- get-header-menus [params]
  (let [col    (.-column params)
        col-id (.-colId col)]
    (cond (= "trade-pattern" col-id)
          (clj->js [{"name"    "Manage"
                     :action #(rf/dispatch-sync [:navigate (reitit/match-by-name router :about)])}])
          :else (.-defaultItems col))))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/assets/images/warning_clojure.png"}]])

(def view-models
  {:home           {:view #'trades/view}
   :trade-patterns {:view  #'trade-patterns/view
                    :model :trade-patterns}
   :about          {:view #'about-page}})

(defn show []
  (let [{:keys [view]} @(rf/subscribe [:active-view-model])]
    (js/console.log (str "active-view ==> " view))
    [main/view view]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     HistoryEventType/NAVIGATE
     (fn [event]
       (let [uri (or (not-empty (string/replace (.-token event) #"^.*#" "")) "/")]
         (rf/dispatch
          [:navigate (reitit/match-by-path router uri)]))))
    (.setEnabled true)))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))

(defn start []
  (rf/clear-subscription-cache!)
  (r.dom/render [#'show] (.getElementById js/document "app")))

(defn ^:export init []
  (rf/dispatch-sync [:set-view-models view-models])
  (rf/dispatch-sync [:navigate (reitit/match-by-name router :home)])
  (hook-browser-navigation!)
  (start))
