(ns reason-alpha.core
  (:require [clojure.string :as string]
            [day8.re-frame.http-fx]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [ra-datagrid.core]
            [re-frame.core :as rf]
            [reagent.dom :as r.dom]
            [reason-alpha.events]
            [reason-alpha.events.account-profile]
            [reason-alpha.events.alerts]
            [reason-alpha.events.holdings]
            [reason-alpha.events.models]
            [reason-alpha.events.positions]
            [reason-alpha.events.trade-patterns]
            [reason-alpha.subs]
            [reason-alpha.subs.account-profile]
            [reason-alpha.subs.alerts]
            [reason-alpha.subs.financial-instruments]
            [reason-alpha.subs.holdings]
            [reason-alpha.subs.models]
            [reason-alpha.subs.positions]
            [reason-alpha.subs.trade-patterns]
            [reason-alpha.views :as views]
            [reason-alpha.web.api-client :as api-client]
            [reason-alpha.web.handlers :as handlers])
  (:import goog.History))

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
