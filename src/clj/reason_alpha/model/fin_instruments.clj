(ns reason-alpha.model.fin-instruments
  (:require [reason-alpha.model.core :as model :refer [def-model]]))

(def-model Symbol
  :model/symbol
  [:map
   [:symbol/ticker [:string {:min 1}]]
   [:symbol/instrument-id uuid?]
   [:symbol/provider
    [:enum {:enum/titles {:yahoo-finance "Yahoo! Finance"
                          :saxo-dma      "Saxo/DMA"
                          :easy-equities "Easy Equities"}}
                         :yahoo-finance :saxo-dma :easy-equities]]])

(def-model Price
  :model/instrument-price
  [:map
   [:price/creation-id uuid?]
   [:price/id uuid?]
   [:price/symbol Symbol]
   [:price/date inst?]
   [:price/open float?]
   [:price/close float?]
   [:price/high float?]
   [:price/low float?]
   [:price/adj-close float?]
   [:price/volume int?]])

(def-model Instrument
  :model/instrument
  [:map
   [:instrument/creation-id uuid?]
   [:instrument/id {:optional true} uuid?]
   [:instrument/name [:string {:min 1}]]
   [:instrument/symbols {:optional true} [:sequential Symbol]]
   [:instrument/type [:enum
                      {:enum/titles {:share    "Share"
                                     :etf      "ETF"
                                     :currency "Currency"
                                     :crypto   "Crypto"}}
                      :share :etf :currency :crypto]]
   [:instrument/currency-instrument-id uuid?]
   [:instrument/prices {:optional true} [:sequential Price]]
   [:instrument/account-id uuid?]])
