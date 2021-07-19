(ns reason-alpha.core
  (:require ["@ag-grid-community/react" :as ag-grd-react]
            ["@ag-grid-enterprise/all-modules" :as ag-grd]
            [cljs-time.core :as t]
            [clojure.string :as string]
            [day8.re-frame.http-fx]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reason-alpha.events]
            [reason-alpha.events.trade-patterns]
            [reason-alpha.subs]
            [reason-alpha.utils :as utils]
            [reason-alpha.views.main :as main]
            [reason-alpha.views.trade-patterns :as trade-patterns]
            [reitit.core :as reitit])
  (:import goog.History))

(def securities #{"IGG"
                  "SHA"
                  "CH8"
                  "CTSH"
                  "DIS"
                  "FB"
                  "GILD"
                  "GLD"
                  "GSK"
                  "INTC"
                  "PTEC"})

(defn get-trades [quantity]
  (for [day (range quantity)]
    {:open-date                            (t/minus (t/now) (t/days day))
     :trade-pattern                        "Breakout - Entering Preceding Base"
     :security                             (securities day)
     :long-short                           (if (odd? day) "L" "S")
     :trading-time-frame                   "Day"
     :quantity                             (utils/rand-between 100 100000)
     :open                                 (utils/rand-between 1 300)
     :close                                (utils/rand-between 1 300)
     :currency                             "USD"
     :cost                                 (utils/rand-between 5 20)
     :interest-per-day                     (utils/rand-between 1 10)
     :interest-total                       (utils/rand-between 1 10)
     :total-cost                           (utils/rand-between 10 30)
     :profit-target                        (utils/rand-between 1 300)
     :profit-target-total                  (utils/rand-between 1 1000)
     :profit-target-percent                (utils/rand-between 1 100)
     :profit-target-total-incl-costs       (utils/rand-between 1 1000)
     :profit-target-percent-incl-costs     (utils/rand-between 1 100)
     :close-date                           (t/now)
     :profit-loss                          (utils/rand-between -1000 1000)
     :profit-loss-incl-costs               (utils/rand-between -1000 1000)
     :profit-loss-home-currency            (utils/rand-between -1000 1000)
     :profit-loss-incl-costs-home-currency (utils/rand-between -1000 1000)
     :profit-loss-percent-risk             (utils/rand-between -100 100)
     :stop                                 (utils/rand-between 1 300)
     :stop-loss-total                      (utils/rand-between -300 300)
     :loss-percent                         (utils/rand-between -100 1)
     :first-deviation                      (rand 100)
     :first-deviation-stop                 (rand 100)
     :conversion-rate-home-currency        (rand 100)}))

(def state {:columns [{:headerName "Open Date"
                       :field      "open-date"}
                      {:headerName "Trade Pattern"
                       :field      "trade-pattern"}
                      {:headerName "Security"
                       :field      "security"}
                      {:headerName "Long/Short"
                       :field      "long-short"}
                      {:headerName "Trading Time Frame"
                       :field      "trading-time-frame"}
                      {:headerName "Quantity"
                       :field      "quantity"}
                      {:headerName "Open"
                       :field      "open"}
                      {:headerName "Close"
                       :field      "close"}
                      {:headerName "Currency"
                       :field      "currency"}
                      {:headerName "Cost"
                       :field      "cost"}
                      {:headerName "Interest/ Day"
                       :field      "interest-per-day"}
                      {:headerName "Interest Total"
                       :field      "interest-total"}
                      {:headerName "Total Cost"
                       :field      "total-cost"}
                      {:headerName "Profit Target"
                       :field      "profit-target"}
                      {:headerName "Profit Target Total"
                       :field      "profit-target-total"}
                      {:headerName "Profit Target %"
                       :field      "profit-target-percent"}
                      {:headerName "Profit Target Total Incl. Costs"
                       :field      "profit-target-total-incl-costs"}
                      {:headerName "Profit Target % Incl. Costs"
                       :field      "profit-target-percent-incl-costs"}
                      {:headerName "Close Date"
                       :field      "close-date"}
                      {:headerName "Profit/Loss"
                       :field      "profit-loss"}
                      {:headerName "Profit/Loss Incl. Costs"
                       :field      "profit-loss-incl-costs"}
                      {:headerName "Profit/Loss (Home Currency)"
                       :field      "profit-loss-home-currency"}
                      {:headerName "Profit/Loss Incl. Costs (Home Currency)"
                       :field      "profit-loss-incl-costs-home-currency"}
                      {:headerName "Profit/Loss % Risk"
                       :field      "profit-loss-percent-risk"}
                      {:headerName "stop"
                       :field      "stop"}
                      {:headerName "Stop Loss Total"
                       :field      "stop-loss-total"}
                      {:headerName "Loss %"
                       :field      "loss-percent"}
                      {:headerName "1st Deviation"
                       :field      "first-deviation"}
                      {:headerName "1st Deviation Stop"
                       :field      "first-deviation-stop"}
                      {:headerName "Conversion Rate (Home Currency)"
                       :field      "conversion-rate-home-currency"}]
            :data (get-trades 100)})

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

(defn home-page []
  [:div.ag-theme-balham-dark {:style {:width  "100%"
                                      :height "100%"}}
   [:> ag-grd-react/AgGridReact
    {:columnDefs       (:columns state)
     :rowData          (:data state)
     :modules          ag-grd/AllModules
     :getMainMenuItems (partial get-header-menu-items {"trade-pattern" ["Edit" :trade-patterns]})}]])

(def view-models
  {:home           {:view #'home-page}
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
  (r/render [#'show] (.getElementById js/document "app")))

(defn ^:export init []
  (rf/dispatch-sync [:set-view-models view-models])
  (rf/dispatch-sync [:navigate (reitit/match-by-name router :home)])
  (hook-browser-navigation!)
  (start))
