(ns reason-alpha.events.alerts
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]))

(rf/reg-event-db
 :alert/send
 (fn [db [_ alert]]
   (-> db
       (get-in data/alerts)
       (as-> a (take 2 a))
       list
       (conj alert)
       (as-> a (assoc-in db data/alerts a)))))

(rf/reg-event-db
 :alert/close
 (fn [db [_ rid]]
   (->> data/alerts
       (get-in db)
       (remove (fn [{:keys [result-id]}]
                 (= result-id rid)))
       (assoc-in db data/alerts))))
