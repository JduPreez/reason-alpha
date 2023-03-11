(ns reason-alpha.events.account-profile
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :account-profile/load
 (fn [{:keys [db]} _]
   {#_#_:trade-pattern.query/getn nil
    :dispatch                     [:model.query/getn
                                   [:model/currency]]}))
