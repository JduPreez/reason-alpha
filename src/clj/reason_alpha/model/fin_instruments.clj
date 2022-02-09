(ns reason-alpha.model.fin-instruments
  (:require [reason-alpha.model.core :as model :refer [def-model]]))

(def-model Symbol
  ^:symbol
  [:map
   [:symbol/ticker [:string {:min 1}]]
   [:symbol/instrument-id uuid?]
   [:symbol/provider [:enum :yahoo-finance :saxo-dma :easy-equities]]])

(def-model Price
  ^:instrument-price
  [:map
   [:price/creation-id uuid?]
   [:price/id uuid?]
   [:price/symbol Symbol]
   [:price/date inst?]
   [:price/open decimal?]
   [:price/close decimal?]
   [:price/high decimal?]
   [:price/low decimal?]
   [:price/adj-close decimal?]
   [:price/volume int?]])

(def-model Instrument
  ^:instrument
  [:map
   [:instrument/creation-id uuid?]
   [:instrument/id {:optional true} uuid?]
   [:instrument/name [:string {:min 1}]]
   [:instrument/symbols {:optional true} [:sequential Symbol]]
   [:instrument/type [:enum :share :etf :currency :crypto]]
   [:instrument/currency-instrument-id uuid?]
   [:instrument/prices {:optional true} [:sequential Price]]])