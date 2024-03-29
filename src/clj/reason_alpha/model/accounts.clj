(ns reason-alpha.model.accounts
  (:require [reason-alpha.model.core :as model :refer [def-model]]
            [reason-alpha.model.fin-instruments :as fin-instruments]
            [reason-alpha.model.utils :as mutils]))

(def-model Profile
  :model/profile
  [:map
   [:profile/id {:optional true} uuid?]
   [:profile/email string?]
   [:profile/name {:optional true} string?]
   #_[:profile/first-name {:optional true} string?]
   #_[:profile/last-name {:optional true} string?]
   [:profile/image {:optional true} string?]])

(def-model Subscriptions
  :model/subscriptions
  [:map
   [:subscription/marketstack {:optional true}
    [:map
     [:access-key string?]]]])

(def-model Account
  :model/account
  [:map
   [:account/id {:optional true} uuid?]
   [:account/user-id string?]
   [:account/user-name string?]
   [:account/currency fin-instruments/Currency]
   [:account/subscriptions {:optional true} Subscriptions]
   [:account/profile {:optional true} Profile]])

(def-model AccountDto
  :model/account-dto
  [:map
   [:account-id {:command-path [:account/id]} uuid?]
   [:account-creation-id {:command-path [:account/creation-id]}
    uuid?]
   [:user-id {:command-path [:account/user-id]} string?]
   [:user-name {:title        "User Name"
                :command-path [:account/user-name]
                :optional     true} string?]
   [:email {:title        "E-mail"
            :command-path [:account/profile :profile/email]} string?]
   [:name {:title        "Name"
           :optional     true
           :command-path [:account/profile :profile/name]} string?]
   [:account-currency {:title        "Main Currency"
                       :ref          :account/currency
                       :command-path [[:account/currency]
                                      [:account/currency-name]]}
    [:tuple keyword? string?]]
   [:marketstack-access-key
    {:title        "Marketstack Subscription"
     :command-path [:account/subscriptions
                    :subscription/marketstack
                    :access-key]}
    string?]])
