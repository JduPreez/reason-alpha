(ns reason-alpha.integration.market-data.model.fin-instruments
  (:require [reason-alpha.model.core :as model :refer [def-model]]))

(def-model Price
  ::price
  [:map
   [:price/id {:optional true} uuid?]
   [:price/creation-id uuid?]
   [:price/symbol-ticker string?]
   [:price/symbol-provider keyword?]
   [:price/time inst?]
   [:price/open number?]
   [:price/close number?]
   [:price/high number?]
   [:price/low number?]
   [:price/volume int?]
   [:price/adj-open {:optional true} [:maybe number?]]
   [:price/adj-high {:optional true} [:maybe number?]]
   [:price/adj-low {:optional true} [:maybe number?]]
   [:price/adj-close {:optional true} [:maybe number?]]
   [:price/adj-volume {:optional true} [:maybe int?]]])

(def-model SharePricesResult
  ::prices-result
  [:map
   [:result-id uuid?]
   [:result {:optional true} [:sequential Price]]
   [:type [:enum :error :success :warn :info :failed-validation]]
   [:title {:optional true} string?]
   [:error {:optional true} any?]
   [:description {:optional true}
    [:or string? [:sequential string?]]]
   [:nr-items {:optional true} int?]])

(def-model Currency
  ::currency
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
  ::currency-pair
  [:tuple
   Currency Currency])

(def-model ExchangeRate
  ::exchange-rate
  [:map
   [:exchange-rate/creation-id uuid?]
   [:exchange-rate/id {:optional true} uuid?]
   [:exchange-rate/base-currency Currency]
   [:exchange-rate/other-currency Currency]
   [:exchange-rate/rate number?]
   [:exchange-rate/date-time inst?]])
