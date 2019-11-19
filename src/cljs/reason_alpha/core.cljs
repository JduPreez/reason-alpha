(ns reason-alpha.core
  (:require [day8.re-frame.http-fx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [reitit.core :as reitit]
            [clojure.string :as string])
  (:import goog.History))



(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/assets/images/warning_clojure.png"}]])

(defn home-page []
  [:section.section>div.container>div.content
   (when-let [docs @(rf/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

(def pages
  {:home  #'home-page
   :about #'about-page})

(defn page []
  [:div
   [(pages @(rf/subscribe [:page]))]])
;; -------------------------
;; Routes

(def router
  (reitit/router
   [["/" :home]
    ["/about" :about]]))
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

;; -------------------------
;; Initialize app
(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:navigate (reitit/match-by-name router :home)]) 
  (rf/dispatch [:fetch-docs])
  (hook-browser-navigation!)
  (mount-components))
