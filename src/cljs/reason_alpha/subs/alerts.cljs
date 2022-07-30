(ns reason-alpha.subs.alerts
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]))

(rf/reg-sub
 :alert/list
 (fn [db _]
   #_(get-in db data/alerts)
   [{:type        :error
     :title       "Some error"
     :description "dhjdjhjhd"
     :error       "dddd"}]))

