(ns reason-alpha.model.accounts
  (:require [reason-alpha.model.core :as model :refer [def-model]]
            [reason-alpha.model.fin-instruments :as fin-instruments]))

(def-model Profile
  :model/profile
  [:map
   [:profile/id {:optional true} uuid?]
   [:profile/email string?]
   [:profile/first-name {:optional true} string?]
   [:profile/last-name {:optional true} string?]
   [:profile/image {:optional true} string?]])

(def-model Subscriptions
  :model/subscriptions
  [:map
   [:subscription/eod-historical-data {:optional true}
    [:map
     [:api-token string?]]]])

(def-model Account
  :model/account
  [:map
   [:account/id {:optional true} uuid?]
   [:account/user-id string?]
   [:account/user-name string?]
   [:account/currency {:optional true} fin-instruments/Currency]
   [:account/subscriptions {:optional true} Subscriptions]
   [:account/profile {:optional true} Profile]])

(def AccountDto
  [:map
   [:account-id uuid?]
   [:user-id string?]
   [:user-name string?]
   [:email string?]
   [:first-name {:optional true} string?]
   [:last-name {:optional true} string?]
   [:image {:optional true} string?]])
