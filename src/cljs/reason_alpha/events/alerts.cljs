(ns reason-alpha.events.alerts
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]))

(rf/reg-event-db
 :alert/send
 (fn [db [evt alert]]
   (-> db
       (get-in data/alerts)
       (or '())
       (conj alert)
       (as-> a (assoc-in db data/alerts a)))))
