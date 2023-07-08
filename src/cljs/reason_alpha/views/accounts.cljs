(ns reason-alpha.views.accounts
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [reagent.dom :as rd]
            [reason-alpha.views.components :as components]
            [reagent.core :as reagent]))

;; https://stackoverflow.com/questions/27163184/how-to-attach-bootstrap-tooltip-to-clojurescript-reagent-component
;; https://ericnormand.me/guide/re-frame-lifecycle#component-did-mount
(defn view []
  (ra/create-class
   {:component-did-mount
    , (fn [_]
        (-> "#eod-token-help"
            js/$
            (.popover #js {:html true})))
    :reagent-render
    , (fn []
        (let [*name         (rf/subscribe [:view.data ::account-edit :name])
              *email        (rf/subscribe [:view.data ::account-edit :email])
              *eod-token    (rf/subscribe [:view.data ::account-edit :eod-historical-data-api-token])
              *acc-currency (rf/subscribe [:view.data ::account-edit :account-currency])
              *acc-schema   (rf/subscribe [:model :model/account-dto])
              *schema       (rf/subscribe [:model/members-of :model/account-dto :account-currency])]
          (cljs.pprint/pprint [:SELECTEDXXX @*acc-schema])
          [:div.card
           [:div.card-status.bg-primary.br-tr-3.br-tl-3]
           [:div.card-header
            [:h3.card-title "Account Profile"]]
           [:form.card
            [:div.card-body
             [:div.row
              [:div.col-md-6.col-lg-6
               [:div.form-group
                [:label.form-label "Name"]
                [:input.form-control {:type         "text"
                                      :disabled     true
                                      :defaultValue ""
                                      :placeholder  @*name}]]
               [:div.form-group
                [:label.form-label "E-mail"]
                [:input.form-control {:type         "text"
                                      :disabled     true
                                      :defaultValue ""
                                      :placeholder  @*email}]]]
              [:div.col-md-6.col-lg-6
               [:div.form-group
                [:label.form-label "Main Currency"]
                [components/select-dropdown
                 *acc-currency
                 :view              ::account-edit
                 :schema            @*acc-schema
                 :model-type        :account
                 :member-nm         :account-currency
                 :data-subscription [:financial-instrument/currencies]
                 :selected          :view.data/update]]
               [:div.form-group
                [:label.form-label "EOD Historical Data Subscription"]
                [:div.row.gutters-sm
                 [:div.col
                  [:input.form-control {:type        "text"
                                        :placeholder "EOD Historical Data API Token"
                                        :value       @*eod-token
                                        :on-change   #(rf/dispatch [:view.data/update
                                                                    ::account-edit
                                                                    :eod-historical-data-api-token
                                                                    (-> % .-target .-value)])}]]
                 [:span.col-auto.align-self-center
                  [:span.form-help.bg-primary.text-white
                   {:id                  "eod-token-help"
                    :data-toggle         "popover"
                    :data-content        "<span>Your EOD Historical Data API Token</span>"
                    :data-placement      "top"
                    :data-original-title nil
                    :title               "test"}
                   "?"]]]]]]]
            [:div.card-footer.text-right
             [:div.d-flex
              [:a.btn.btn-link {:href     "#"
                                :on-click #(do
                                             (rf/dispatch [:close-active-form])
                                             (.preventDefault %))}
               "Cancel"]
              [:button.btn.btn-primary.ml-auto {:type     "button"
                                                :on-click #(do
                                                             (.preventDefault %)
                                                             (rf/dispatch [:view.data/save
                                                                           ::account-edit
                                                                           :account/save]))}
               "Save"]]]]]))}))
