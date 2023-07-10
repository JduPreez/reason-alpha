(ns reason-alpha.subs
  (:require [cljs-time.core :as t]
            [re-frame.core :as rf]
            [reason-alpha.utils :as utils]
            [reason-alpha.data :as data]))

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
 (fn [db [_ view field]]
   (data/get-view-data db view field)))
