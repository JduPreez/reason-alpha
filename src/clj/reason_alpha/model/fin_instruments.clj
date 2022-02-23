(ns reason-alpha.model.fin-instruments
  (:require [reason-alpha.model.core :as model :refer [def-model]]))

(def-model Provider
  :model/provider
  [:enum {:provider/titles {:yahoo-finance "Yahoo! Finance"
                            :saxo-dma      "Saxo/DMA"
                            :easy-equities "Easy Equities"}}
   :yahoo-finance :saxo-dma :easy-equities])

(def-model Symbol
  :model/symbol
  [:map
   [:symbol/ticker [:string {:min 1}]]
   [:symbol/instrument-id uuid?]
   [:symbol/provider [:ref :model/provider]]])

(def-model Price
  :model/instrument-price
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
  :model/instrument
  [:map
   [:instrument/creation-id uuid?]
   [:instrument/id {:optional true} uuid?]
   [:instrument/name [:string {:min 1}]]
   [:instrument/symbols {:optional true} [:sequential Symbol]]
   [:instrument/type [:enum :share :etf :currency :crypto]]
   [:instrument/currency-instrument-id uuid?]
   [:instrument/prices {:optional true} [:sequential Price]]
   [:instrument/account-id uuid?]])
