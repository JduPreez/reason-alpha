(ns reason-alpha.model.fin-instruments
  (:require [malli.core :as m]
            [reason-alpha.model.core :as model :refer [def-model]]))

(def-model Symbol
  :model/symbol
  [:map
   [:symbol/ticker {:min 1} string?]
   [:symbol/holding-id {:optional true} uuid?]
   [:symbol/provider
    [:enum {:enum/titles {:marketstack   "Marketstack"
                          :saxo-dma      "Saxo/DMA"
                          :easy-equities "Easy Equities"}}
                         :marketstack :saxo-dma :easy-equities]]])

(def-model Price
  :model/price
  [:map
   [:price/creation-id uuid?]
   [:price/id uuid?]
   [:price/symbol Symbol] ;; Symbol or Instrument ID?
   [:price/time inst?]
   [:price/open float?]
   [:price/close float?]
   [:price/previous-close float?]
   [:price/high float?]
   [:price/low float?]
   [:price/adj-close float?]
   [:price/volume int?]
   [:price/change float?]])

(def-model PriceDto
  :model/price-dto
  [:map
   [:price-id {:optional true} uuid?]
   [:price-creation-id uuid?]
   [:symbol-ticker string?]
   [:symbol-provider keyword?]
   [:holding-id uuid?]
   [:price-time inst?]
   [:price-open float?]
   [:price-close float?]
   [:price-high float?]
   [:price-low float?]
   [:price-previous-close float?]
   [:price-volume int?]
   [:price-change float?]])

(def-model Currency
  :model/currency
  [:enum {:enum/titles {:AED "AED",
                        :AUD "AUD",
                        :BRL "BRL",
                        :CAD "CAD",
                        :CHF "CHF",
                        :CLP "CLP",
                        :CNY "CNY",
                        :COP "COP",
                        :CZK "CZK",
                        :DKK "DKK",
                        :EUR "EUR",
                        :GBP "GBP",
                        :HKD "HKD",
                        :HUF "HUF",
                        :IDR "IDR",
                        :ILS "ILS",
                        :INR "INR",
                        :JPY "JPY",
                        :KRW "KRW",
                        :MXN "MXN",
                        :MYR "MYR",
                        :NOK "NOK",
                        :NZD "NZD",
                        :PHP "PHP",
                        :PLN "PLN",
                        :RON "RON",
                        :RUB "RUB",
                        :SAR "SAR",
                        :SEK "SEK",
                        :SGD "SGD",
                        :THB "THB",
                        :TRY "TRY",
                        :TWD "TWD",
                        :USD "USD",
                        :ZAR "ZAR"}}
   :USD :EUR :JPY :GBP :AUD :CAD :CHF :CNY :HKD :NZD :SEK
   :KRW :SGD :NOK :MXN :INR :RUB :ZAR :TRY :BRL :TWD :DKK
   :PLN :THB :IDR :HUF :CZK :ILS :CLP :PHP :AED :COP :SAR
   :MYR :RON])

(def-model CurrencyPair
  :model/currency-pair
  [:tuple
   Currency Currency])

(def-model ExchangeRate
  :model/exchange-rate
  [:map
   [:exchange-rate/creation-id uuid?]
   [:exchange-rate/id {:optional true} uuid?]
   [:exchange-rate/base-currency Currency]
   [:exchange-rate/other-currency Currency]
   [:exchange-rate/rate number?]
   [:exchange-rate/date-time inst?]])

(def-model ExchangeRateDto
  :model/exchange-rate-dto
  [:map
   [:exchange-rate-creation-id uuid?]
   [:exchange-rate-id {:optional true} uuid?]
   [:base-currency Currency]
   [:other-currency Currency]
   [:rate number?]
   [:date-time inst?]])
