(ns reason-alpha.events.instruments
  (:require [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.model.utils :as model.utils]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.api-client :as api-client]))

(rf/reg-event-fx
 :instrument/success
 (fn-traced [_ [_ result]]
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

#_(rf/reg-event-fx
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
 :model.command/save!-result
 (fn [db [_ result]]
   (data/save-local! {:db         db
                      :model-type :instrument
                      :data       result})))

(rf/reg-fx
 :instrument.command/save!
 (fn [instr]
   (cljs.pprint/pprint {:instrument.command/save! instr})
   #_(data/save-remote! {:command       :instrument.command/save!
                         :data          instr
                         :success-event [:instrument/save-success]})))

(rf/reg-event-fx
 :instrument.command/create
 (fn [{:keys [db]} [_ {:keys [creation-id] :as new-instr}]]
   (let [new-instr       (if creation-id
                           new-instr
                           (assoc new-instr :instrument-creation-id (utils/new-uuid)))
         query-dao-model (get-in db (data/model :model/instrument-dao))
         cmd-instr       (mapping/query-dao->command-ent query-dao-model new-instr)
         db              (data/save-local! {:model-type :instrument
                                            :data       new-instr
                                            :db         db})]
     {:db                       db
      :instrument.command/save! cmd-instr})))

(rf/reg-event-fx
 :instrument/save-success
 (fn [_ [evt {:keys [type] :as result}]]
   (if (= type :success)
     ;;(data/save-local! result)
     (utils/log evt result))))

;; (rf/reg-event-fx
;;  :instrument.command/save!
;;  (fn [_ [evt instr]]
;;    (cljs.pprint/pprint {::save! instr})
;;    (let [fx (data/save-local! {:db         db
;;                                :model-type :instrument
;;                                :data       instr})])
;;    #_(data/save-remote! {:command evt
;;                          :entity  instr})))
