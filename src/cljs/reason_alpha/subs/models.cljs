(ns reason-alpha.subs.models
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]))

(rf/reg-sub
 :models
 (fn [db _]
   (get-in db data/models)))

(rf/reg-sub
 :model
 (fn [db [_ _model-k]]
   (rf/subscribe [:models]))
 (fn [models [_ model-k]]
   (get models model-k)))
