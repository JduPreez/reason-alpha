(ns reason-alpha.subs
  (:require [cljs-time.core :as t]
            [re-frame.core :as rf]
            [reason-alpha.utils :as utils]))

(rf/reg-sub
 :active-view-model     ;; usage: (subscribe [:active-view])
 (fn [db _]             ;; db is the (map) value stored in the app-db atom
   (:active-view-model db)))  ;; extract a value from the application state

(rf/reg-sub
 :loading
 (fn [db [_ key']]
   {key' (true?
          (get-in db [:loading key']))}))

(rf/reg-sub
 :current-route
 (fn [db]
   (:current-route db)))
