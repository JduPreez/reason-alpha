(ns reason-alpha.subs
  (:require [cljs-time.core :as t]
            [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.model.validation :as validation]
            [reason-alpha.utils :as utils]
            [reagent.core :as reagent]))

(rf/reg-sub
 :active-view-model     ;; usage: (subscribe [:active-view])
 (fn [db _] ;; db is the (map) value stored in the app-db atom
   (get-in db data/active-view-model)))  ;; extract a value from the application state

(rf/reg-sub
 :loading
 (fn [db [_ key']]
   {key' (true?
          (get-in db [:loading key']))}))

(rf/reg-sub
 :current-route
 (fn [db]
   (get-in db data/current-route)))

(rf/reg-sub
 :view.data
 (fn [db [_ view & [field]]]
   (if field
     (data/get-view-data db view field)
     (data/get-view-data db view))))

(rf/reg-sub
 :view.data/with-validation
 (fn [[_ model-type view _]]
   [(rf/subscribe [:model model-type])
    (rf/subscribe [:view.data view])])
 (fn [[schema view-data] [_ _ _ member]]
   (when (and schema view-data)
     (let [vres (validation/validate schema view-data member)]
       (cond-> {:type :success
                :result (member view-data)}
         vres (merge vres))))))

(rf/reg-sub
 :view.data/valid?
 (fn [[_ model-type view]]
   [(rf/subscribe [:model model-type])
    (rf/subscribe [:view.data view])])
 (fn [[schema view-data] [_ _ _]]
   (when (and schema view-data)
     (validation/validate schema view-data))))
