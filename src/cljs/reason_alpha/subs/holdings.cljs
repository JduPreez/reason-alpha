(ns reason-alpha.subs.holdings
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]))

(rf/reg-sub
 :holdings
 (fn [db _]
   (sort-by :instrument/name
            (get-in db data/holdings))))
