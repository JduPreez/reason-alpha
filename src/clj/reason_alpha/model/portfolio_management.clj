(ns reason-alpha.model.portfolio-management
  (:require [reason-alpha.model.core :as model :refer [def-model]]))

(def-model TradePattern
  ^:trade-pattern
  [:map
   [:trade-pattern/creation-id uuid?]
   [:trade-pattern/id {:optional true} uuid?]
   [:trade-pattern/name [:string {:min 1}]]
   [:trade-pattern/description {:optional true} [:string {:min 1}]]])

(def-model Symbol
  ^:symbol
  [:map
   [:symbol/ticker [:string {:min 1}]]
   [:symbol/provider [:enum :yahoo-finance :saxo-dma :easy-equities]]])

(def-model Instrument
  ^:instrument
  [:map
   [:instrument/creation-id uuid?]
   [:instrument/id {:optional true} uuid?]
   [:instrument/name [:string {:min 1}]]
   [:instrument/symbols [:vector ::Symbol]]
   [:instrument/type]
   [:instrument/currency]])

(def-model Transaction
  ^:transaction
  [:map
   [:transaction/creation-id uuid?]
   [:transaction/id {:optional true} uuid?]
   [:transaction/type [:enum :buy :sell :dividend :reinvest-divi]]
   [:transaction/time inst?]
   [:transaction/quantity decimal?]
   [:transaction/price decimal?]
   [:transaction/fees {:optional true} decimal?]
   [:instrument/id uuid?]])
