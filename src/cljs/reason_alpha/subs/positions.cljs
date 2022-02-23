(ns reason-alpha.subs.positions
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]))

(rf/reg-sub
 :positions
 (fn [db _]
   (sort-by :instrument-name
            (get-in db data/holdings))))
