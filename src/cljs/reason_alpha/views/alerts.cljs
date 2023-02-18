(ns reason-alpha.views.alerts
  (:require [re-frame.core :as rf]))

(defn view []
  (fn []
    (let[*alerts (rf/subscribe [:alert/list])]
      [:<>
       (for [{:keys [type description
                     error title result-id]} @*alerts]
         ^{:key (str result-id)}
         [:div.row {:style {:margin-top "10px"}}
          (case type
            (:error
             :failed-validation) [:div.alert-card.card
                                  [:div.alert.alert-danger
                                   ;;{:margin-bottom "0 !important"}
                                   [:strong description]
                                   [:a.btn.btn-xs.btn-danger.float-right
                                    {:href     "#"
                                     :on-click #(do (.preventDefault %)
                                                    (rf/dispatch [:alert/close result-id]))}
                                    title]]]
            (:success :info)     [:div.alert-card.card
                                  [:div.alert.alert-success
                                   ;;{:margin-bottom "0 !important"}
                                   [:strong description]
                                   [:a.btn.btn-xs.btn-success.float-right
                                    {:href     "#"
                                     :on-click #(do (.preventDefault %)
                                                    (rf/dispatch [:alert/close result-id]))}
                                    title]]]
            [:div.alert-card.card
             [:div.alert.alert-warning
              ;;{:margin-bottom "0 !important"}
              [:strong description]
              [:a.btn.btn-xs.btn-warning.float-right
               {:href     "#"
                :on-click #(do (.preventDefault %)
                               (rf/dispatch [:alert/close result-id]))}
               title]]])])])))
