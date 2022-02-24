(ns reason-alpha.subs.models
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.model.utils :as model.utils]))

(rf/reg-sub
 :models
 (fn [db _]
   (get-in db data/models)))

(rf/reg-sub
 :model
 :<- [:models]
 (fn [models [_ model-k]]
   (get models model-k)))

(rf/reg-sub
 :models/members-of
 (fn [_ [_ model-k _member-k]]
   (rf/subscribe [:model model-k]))
 (fn [model [_ _model-k member-k]]
   (model.utils/get-model-members-of model member-k)))
