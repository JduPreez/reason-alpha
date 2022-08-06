(ns reason-alpha.subs.alerts
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]))

(rf/reg-sub
 :alert/list
 (fn [db _]
   (get-in db data/alerts)))

