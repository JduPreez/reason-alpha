(ns reason-alpha.subs.positions
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.web.api-client :as api-client]
            [reason-alpha.utils :as utils]))

(rf/reg-sub
 :position/list
 (fn [db _]
   (sort-by (juxt :instrument-name :open-time)
            (get-in db data/positions))))
