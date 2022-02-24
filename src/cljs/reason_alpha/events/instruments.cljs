(ns reason-alpha.events.instruments
  (:require [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [re-frame.core :as rf]
            [reason-alpha.web.api-client :as api-client]))

(rf/reg-event-fx
 :instrument/success
 (fn-traced [_ [_ result]]
   (cljs.pprint/pprint {:instrument/success result})
   #_{:dispatch [:save-local :instrument result]}))

(rf/reg-fx
 :instrument.query/getn
 (fn [_]
   (cljs.pprint/pprint {:instrument.query/getn _})
   (api-client/chsk-send! [:model.query/getn [:model/instrument :model/symbol]])
   (api-client/chsk-send! [:instrument.query/getn]
                          {:on-success [:instrument/success]})))

(rf/reg-event-fx
 :instrument.command/add
 (fn-traced [{:keys [db]} _]
   {:db db}))
