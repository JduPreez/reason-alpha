(ns reason-alpha.model.account
  [reason-alpha.model.core :as model :refer [def-model]])

(def-model Profile
  ^:profile
  [:map
   [:profile/creation-id uuid?]
   [:profile/id {:optional true} uuid?]
   [:profile/email string?]
   [:profile/name string?]
   [:profile/image {:optional true} string?]])

(def-model Account
  ^:account
  [:map
   [:account/creation-id uuid?]
   [:account/id {:optional true} uuid?]
   [:account/user-id string?]
   [:account/user-name string?]
   [:account/profile {:optional true} Profile]])
