(ns reason-alpha.model.account
  (:require [reason-alpha.model.core :as model :refer [def-model]]))

(def-model Profile
  :model/profile
  [:map
   [:profile/creation-id uuid?]
   [:profile/id {:optional true} uuid?]
   [:profile/email string?]
   [:profile/name string?]
   [:profile/image {:optional true} string?]])

(def-model Account
  :model/account
  [:map
   [:account/creation-id uuid?]
   [:account/id {:optional true} uuid?]
   [:account/user-id string?]
   [:account/user-name string?]
   [:account/profile {:optional true} Profile]])

(def AccountDto
  [:map
   [:user-id string?]
   [:user-name string?]
   [:email string?]
   [:name {:optional true} string?]
   [:image {:optional true} string?]])
