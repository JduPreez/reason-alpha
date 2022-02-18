(ns reason-alpha.events.holdings
  (:require [re-frame.core :as rf]))

(rf/reg-fx
 :holding.query/getn
 (fn [_]
   (cljs.pprint/pprint {:trade-pattern.query/getn _})
   (api-client/chsk-send! [:trade-pattern.query/getn]
                          {:on-success [:trade-pattern/success]})))
