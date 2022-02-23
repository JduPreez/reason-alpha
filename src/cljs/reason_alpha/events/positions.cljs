(ns reason-alpha.events.positions
  (:require [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [re-frame.core :as rf]
            [reason-alpha.web.api-client :as api-client]))

(rf/reg-fx
 :position.query/getn
 (fn [_]
   (cljs.pprint/pprint {:position.query/getn _})
   (api-client/chsk-send! [:position.query/getn]
                          {:on-success [:position/success]})))

(rf/reg-event-fx
 :position.command/add
 (fn [{:keys [db]} _]
   {:db db}))
