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

(rf/reg-event-fx
 :instrument/load
 (fn [{:keys [db]}]
   (cljs.pprint/pprint :instrument/load)
   {:instrument.query/getn nil
    :dispatch              [:model.query/getn
                            [:model/instrument :model/symbol
                             :model/instrument-dao]]}))

(rf/reg-fx
 :instrument.query/getn
 (fn [_]
   (cljs.pprint/pprint :instrument.query/getn)
   (api-client/chsk-send! [:instrument.query/getn]
                          {:on-success [:instrument/success]})))

(rf/reg-fx
 :instrument.query/get1
 (fn [instr-id]
   (api-client/chsk-send! [:instrument.query/get1 {:instrument-id instr-id}])))

(rf/reg-event-fx
 :instrument.command/add
 (fn [{:keys [db]} _]
   (let [instrs      (get-in db (data/entity-data :instrument))
         creation-id (utils/new-uuid)
         new-instr   {:instrument-creation-id creation-id}]
     {:dispatch [:edit [new-instr]]
      :db       (update-in db
                           data/instruments
                           #(conj % new-instr))})))

(rf/reg-event-db
 :instrument.command/create
 (fn [db [_ new-instrument]]
   (let [updated-db (data/save-local! {:model-type :instrument
                                       :data       new-instrument
                                       :db         db})]
     (data/save-remote! {:command       :instrument.command/save!
                         :data          new-instrument
                         :success-event [:instrument/save-success]})
     updated-db)))

(rf/reg-event-fx
 :instrument/save-success
 (fn [_ [evt {:keys [type] :as result}]]
   (if (= type :success)
     ;;(data/save-local! result)
     (utils/log evt result))))

