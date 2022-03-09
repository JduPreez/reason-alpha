(ns reason-alpha.model.fin-instruments
  (:require [reason-alpha.model.core :as model :refer [def-model]]
            [reason-alpha.model.utils :as model.utils]))

(def-model Symbol
  :model/symbol
  [:map
   [:symbol/ticker {:min 1} string?]
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
   [:instrument/account-id {:optional true} uuid?]])

(let [{{ptitles :enum/titles} :properties
       providers              :members} (model.utils/get-model-members-of
                                         Symbol
                                         :symbol/provider)
      symbols-schema                    (for [p    providers
                                              :let [t (get ptitles p)]]
                                          [p {:title        t
                                              :optional     true
                                              :pivot        :symbol/provider
                                              :command-path [:instrument/symbols 0 :symbol/ticker]}
                                           string?])]
  (def-model InstrumentDao
    :model/instrument-dao
    (into
     [:map
      [:instrument-id {:optional     true
                       :command-path [:instrument/id]}
       uuid?]
      [:instrument-creation-id {:command-path [:instrument/creation-id]}
       uuid?]
      [:instrument-name {:title        "Instrument"
                         :optional     true
                         :command-path [:instrument/name]} string?]]
     cat
     [symbols-schema
      [[:instrument-type {:title        "Type"
                          :optional     true
                          :ref          :instrument/type
                          :command-path [:instrument/type]} keyword?]]])))

(comment
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
