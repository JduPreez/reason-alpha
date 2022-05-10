(ns reason-alpha.model.fin-instruments
  (:require [reason-alpha.model.core :as model :refer [def-model]]))

(def-model Symbol
  :model/symbol
  [:map
   [:symbol/ticker {:min 1} string?]
   [:symbol/holding-id {:optional true} uuid?]
   [:symbol/provider
    [:enum {:enum/titles {:eod-historical-data "EOD Historical Data"
                          :saxo-dma            "Saxo/DMA"
                          :easy-equities       "Easy Equities"}}
                         :eod-historical-data :saxo-dma :easy-equities]]])

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

(comment
  (let [x {:USD "USD"
           :EUR "EUR"
           :JPY "JPY"
           :GBP "GBP"
           :AUD "AUD"
           :CAD "CAD"
           :CHF "CHF"
           :CNY "CNY"
           :HKD "HKD"
           :NZD "NZD"
           :SEK "SEK"
           :KRW "KRW"
           :SGD "SGD"
           :NOK "NOK"
           :MXN "MXN"
           :INR "INR"
           :RUB "RUB"
           :ZAR "ZAR"
           :TRY "TRY"
           :BRL "BRL"
           :TWD "TWD"
           :DKK "DKK"
           :PLN "PLN"
           :THB "THB"
           :IDR "IDR"
           :HUF "HUF"
           :CZK "CZK"
           :ILS "ILS"
           :CLP "CLP"
           :PHP "PHP"
           :AED "AED"
           :COP "COP"
           :SAR "SAR"
           :MYR "MYR"
           :RON "RON"}]
    (into (sorted-map) x))

  (letfn [(get-model-members-of [schema member-k]
            (let [member        (->> schema
                                     rest
                                     (some #(when (= member-k (first %)) %)))
                  type-spec     (last member)
                  maybe-props   (second type-spec)
                  has-props?    (map? maybe-props)
                  child-members (if has-props?
                                  (nnext type-spec)
                                  (next type-spec))]
              {:properties maybe-props
               :members    child-members}))]
    (get-model-members-of
     Symbol
     :symbol/provider)
    #_(let [{{ptitles :enum/titles} :properties
             providers              :members} (get-model-members-of
                                               Symbol
                                               :symbol/provider)
            providers-schema                  (for [p    providers
                                                    :let [t (get ptitles p)]]
                                                [p {:title    t
                                                    :optional true} keyword?])]
      (into
       [:map
        [:instrument-name {:title    "Instrument"
                           :optional true} string?]]
       cat
       [providers-schema
        [[:instrument-type {:title    "Type"
                            :optional true} keyword?]]])))

  )
