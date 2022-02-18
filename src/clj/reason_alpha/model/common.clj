(ns reason-alpha.model.common
  (:require [reason-alpha.model.core :as model :refer [def-model]]))

(def-model Result
  ^:result
  [:map
   [:result {:optional true} any?]
   [:type [:enum :error :success :warn :info]]
   [:error {:optional true} any?]
   [:description {:optional true} string?]
   [:nr-items {:optional true} int?]])
