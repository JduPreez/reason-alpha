(ns reason-alpha.model.fin-instruments
  (:require [reason-alpha.model.core :as model :refer [def-model]]
            [reason-alpha.model.utils :as model.utils]))

(def-model Symbol
  :model/symbol
  [:map
   [:symbol/ticker [:string {:min 1}]]
   [:symbol/instrument-id {:optional true} uuid?]
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
   [:price/symbol Symbol] ;; Symbol or Instrument ID?
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

(let [{{ptitles :enum/titles} :properties
       providers              :members} (model.utils/get-model-members-of
                                         Symbol
                                         :symbol/provider)
      symbols-schema                    (for [p    providers
                                              :let [t (get ptitles p)]]
                                          [(keyword "symbol" (name p))
                                           {:title        t
                                            :optional     true
                                            :command-path [:instrument/symbols [[:symbol/ticker]
                                                                                {:symbol/provider p}]]}
                                           string?])]
  ;; HOW TO HANDLE `:symbol/provider` that's actually a constant?
  (def-model InstrumentDao
    :model/instrument-dao
    (into
     [:map
      [:instrument-id {:command-path [:instrument/id]
                       :optional     true}
       uuid?]
      [:instrument-creation-id {:command-path [:instrument/creation-id]}
       uuid?]
      [:instrument-name {:title        "Instrument"
                         :optional     true
                         :command-path [:instrument/name]} string?]]
     cat
     [symbols-schema
      [[:instrument-type {:title    "Type"
                          :optional true
                          :ref      :instrument/type} keyword?]]])))

(comment
  (let [{{ptitles :enum/titles} :properties
         providers              :members} (model.utils/get-model-members-of
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
                            :optional true} keyword?]]]))

  )
