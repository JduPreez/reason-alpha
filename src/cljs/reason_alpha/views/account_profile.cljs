(ns reason-alpha.views.account-profile
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [reagent.dom :as rd]))


;; https://stackoverflow.com/questions/27163184/how-to-attach-bootstrap-tooltip-to-clojurescript-reagent-component
;; https://ericnormand.me/guide/re-frame-lifecycle#component-did-mount
(defn view []
  (ra/create-class
   {:component-did-mount
    , (fn [comp]
        ;; "[data-toggle='popover']"
        (-> (js/$ (rd/dom-node comp))
            (.popover))

        #_(.popover
           (js/$ (rd/dom-node comp))))
    :reagent-render
    , (fn []
        (let [default      {}
              *acc-profile (ra/atom default)]
          (fn []
            (let [*curs (rf/subscribe [:financial-instrument/currencies])
                  curs  (->> @*curs
                             seq
                             (cons [:default "Select one"]))]
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
                    [:input.form-control {:type        "text"
                                          :disabled    true
                                          :value       ""
                                          :placeholder "Name"}]]
                   [:div.form-group
                    [:label.form-label "E-mail"]
                    [:input.form-control {:type        "text"
                                          :disabled    true
                                          :value       ""
                                          :placeholder "E-mail"}]]]
                  [:div.col-md-6.col-lg-6
                   [:div.form-group
                    [:label.form-label "Main Currency"]
                    [:select.form-control.select2.custom-select.select2-hidden-accessible
                     {:data-placeholder "Choose one"}
                     (for[[_ ct] curs]
                       ^{:key ct}
                       [:option {:value ct} ct])]]
                   [:div.form-group
                    [:label.form-label "EOD Historical Data Subscription"]
                    [:div.row.gutters-sm
                     [:div.col
                      [:input.form-control {:type          "text"
                                            :placeholder   "EOD Historical Data API Token"
                                            :value         ""
                                            #_#_:on-change #(swap! registration assoc :username (-> % .-target .-value))}]]
                     [:span.col-auto.align-self-center
                      [:span.form-help.bg-primary.text-white
                       {:id                  "testy"
                        :data-toggle         "popover"
                        :data-content        "Your EOD Historical Data API Token"
                        :data-placement      "top"
                        :data-original-title nil
                        :title               "test"}
                       "?"]]]]]]]
                [:div.card-footer.text-right
                 [:div.d-flex
                  [:a.btn.btn-link {:href     "#"
                                    :on-click #(do (.preventDefault %))}
                   "Cancel"]
                  [:button.btn.btn-primary.ml-auto {:type "submit"}
                   "Save"]]]]]))))}))
