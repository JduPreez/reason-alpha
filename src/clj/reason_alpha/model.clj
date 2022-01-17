(ns reason-alpha.model
  (:require [reason-alpha.services.trade-pattern :as svc.trade-pattern]))

;; TODO: Route queries directly to repo functions. 
(def aggregates
  {:trade-pattern
   {:commands {:save!   svc.trade-pattern/save!
               :delete! svc.trade-pattern/delete!}
    :queries  {:get  svc.trade-pattern/getn
               :get1 svc.trade-pattern/get1}} ;; TODO: Fix get-trade-pattern input args to handle command
   :position
   {:commands {:save! nil}
    :queries  {:get  nil
               :get1 nil}}})
