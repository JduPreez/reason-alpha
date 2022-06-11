(ns reason-alpha.subs.trade-patterns
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]))

(rf/reg-sub
 :trade-pattern/list
 (fn [db _]
   (sort-by :trade-pattern-name
            (get-in db data/trade-patterns))))

(rf/reg-sub
 :trade-pattern/parent-ref
 :<- [:trade-pattern/list]
 (fn [trade-patterns [_ id]]
   (some #(when (= id (:trade-pattern-id %)) %)
         trade-patterns)))

(rf/reg-sub
 :trade-pattern/ref-list
 :<- [:trade-pattern/list]
 (fn [trade-patterns _]
   (map
    (fn [{:keys [trade-pattern-id trade-pattern-name]}]
      {:id    (str trade-pattern-id)
       :label trade-pattern-name})
    trade-patterns)))

(rf/reg-sub
 :trade-pattern/parent-ref-list
 :<- [:trade-pattern/list]
 (fn [trade-patterns _] trade-patterns))
