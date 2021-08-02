(ns reason-alpha.data.model)

(def =>delete!
  [:=>
   [:catn [:vector any?]]
   [:map
    [:was-deleted? boolean?]
    [:num-deleted int?]
    [:message {:optional true} string?]]])
