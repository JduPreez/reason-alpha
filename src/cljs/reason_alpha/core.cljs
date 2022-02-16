(ns reason-alpha.core
  (:require [clojure.string :as string]
            [day8.re-frame.http-fx]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [ra-datagrid.core]
            [re-frame.core :as rf]
            [reagent.dom :as r.dom]
            [reason-alpha.events]
            [reason-alpha.events.trade-patterns]
            [reason-alpha.subs]
            [reason-alpha.views :as views]
            [reason-alpha.views.main :as main]
            [reason-alpha.views.trade-patterns :as trade-patterns]
            [reason-alpha.views.trades :as trades]
            [reason-alpha.web.api-client :as api-client]
            [reitit.core :as reitit]
            [reason-alpha.web.handlers :as handlers])
  (:import goog.History))

#_(defn get-header-menu-items [menu-items params]
  (let [col    (.-column params)
        col-id (.-colId col)]
    (if-let [[name route] (get menu-items col-id)]
      (clj->js [{"name"  name
                 :action #(rf/dispatch-sync [:navigate (reitit/match-by-name router route)])}])
      (.-defaultItems col))))

#_(defn- get-header-menus [params]
  (let [col    (.-column params)
        col-id (.-colId col)]
    (cond (= "trade-pattern" col-id)
          (clj->js [{"name"    "Manage"
                     :action #(rf/dispatch-sync [:navigate (reitit/match-by-name router :about)])}])
          :else (.-defaultItems col))))

#_(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/assets/images/warning_clojure.png"}]])

;; TODO: Move this to `reason-alpha.views`
#_(def view-models
  {:trades         {:view  #'trades/view
                    :model :trade}
   :trade-patterns {:view  #'trade-patterns/view
                    :model :trade-pattern}
   :about          {:view #'about-page}})

(defn stop []
  (js/console.log "stop"))

(defn start []
  (js/console.log "stop"))

(defmethod handlers/-event-msg-handler :chsk/handshake
  [{event :event}]
  (views/init))

(defn ^:export init []
  (rf/clear-subscription-cache!)
  (rf/dispatch-sync [:initialize-db])
  (api-client/start!))
