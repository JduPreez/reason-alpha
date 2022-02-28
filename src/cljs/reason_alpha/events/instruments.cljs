(ns reason-alpha.events.instruments
  (:require [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.model.utils :as model.utils]
            [reason-alpha.web.api-client :as api-client]
            [reason-alpha.utils :as utils]))

(rf/reg-event-fx
 :instrument/success
 (fn-traced [_ [_ result]]
   (cljs.pprint/pprint {:instrument/success result})
   #_{:dispatch [:save-local :instrument result]}))

(rf/reg-fx
 :instrument.query/getn
 (fn [_]
   (cljs.pprint/pprint {:instrument.query/getn _})
   (api-client/chsk-send! [:model.query/getn [:model/instrument :model/symbol :model/instrument-dao]])
   (api-client/chsk-send! [:instrument.query/getn]
                          {:on-success [:instrument/success]})))

(rf/reg-event-fx
 :instrument.command/add
 (fn [{:keys [db]} _]
   (let [instrs        (get-in db (data/entity-data :instrument))
         #_#_instr-dao (-> db
                           (get-in data/models)
                           (get :model/instrument-dao))
         creation-id   (utils/new-uuid)
         new-instr     {:instrument-creation-id creation-id}]
     {:dispatch [:edit [new-instr]]
      :db       (update-in db
                           data/instruments
                           #(conj % new-instr))})))
