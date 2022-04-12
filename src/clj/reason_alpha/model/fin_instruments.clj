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
                         :yahoo-finance :saxo-dma :easy-equities]]])

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
