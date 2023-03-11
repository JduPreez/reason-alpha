(ns reason-alpha.views.account-profile
  (:require [re-frame.core :as rf]))

(defn view []
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
                                  :value       nil
                                  :placeholder "Name"}]]
           [:div.form-group
            [:label.form-label "E-mail"]
            [:input.form-control {:type        "text"
                                  :disabled    true
                                  :value       nil
                                  :placeholder "E-mail"}]]]
          [:div.col-md-6.col-lg-6
           [:div.form-group
            [:label.form-label "Main Currency"]
            [:select.form-control.select2.custom-select.select2-hidden-accessible
             {:data-placeholder "Choose one"}
             (for[[_ ct] curs]
               ^{:key ct}
               [:option {:value ct} ct])]]]]]]])))
