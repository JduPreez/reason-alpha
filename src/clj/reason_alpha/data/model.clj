(ns reason-alpha.data.model
  (:require [reason-alpha.model.core :as model]))

(def =>delete! [:=>
                [:catn [:vector any?]]
                [:map
                 [:was-deleted? boolean?]
                 [:num-deleted int?]
                 [:message {:optional true} string?]]])

(model/+def! {"=>delete!" =>delete!})
