(ns reason-alpha.subscriptions
  (:require [re-frame.core :as rf]
            [reason-alpha.utils :as utils]))

(rf/reg-sub
 :trade-patterns
 (fn [db _]
   #_(utils/log ::trade-patterns (get-in db [:data :trade-pattern]))
   (sort-by :trade-pattern/name
            (get-in db [:data :trade-pattern]))))

(rf/reg-sub
 :trade-pattern-options
 :<- [:trade-patterns]
 (fn [trade-patterns]
   (map (fn [{:keys [trade-pattern/id
                     trade-pattern/name]}]
          {:label name
           :value id}) trade-patterns)))
