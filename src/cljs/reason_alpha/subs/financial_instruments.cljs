(ns reason-alpha.subs.financial-instruments
  (:require [re-frame.core :as rf]
            [reason-alpha.model.utils :as model.utils]))

(rf/reg-sub
 :financial-instrument/currencies
 :<- [:model :reason-alpha.model.fin-instruments/currency]
 (fn [currencies _]
   (model.utils/enum-titles currencies)))
