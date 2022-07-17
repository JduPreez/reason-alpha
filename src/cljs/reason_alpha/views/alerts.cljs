(ns reason-alpha.views.alerts
  (:require [re-frame.core :as rf]))

(defn view []
  (fn []
    (let[*alerts (rf/subscribe [:alerts/list grid-id])
         opts    (or options @*options)]
      [:div.card
       [history-list grid-id title]
       [:div.card-body {:style {:padding-top    0
                                :padding-bottom 0}}
        [ra-datagrid/datagrid (merge default-opts opts) fields]]])))
