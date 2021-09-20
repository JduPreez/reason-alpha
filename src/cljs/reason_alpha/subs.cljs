(ns reason-alpha.subs
  (:require [re-frame.core :as rf]
            [reason-alpha.data.trade-patterns :as data.trade-patterns]))

;; Trade patterns

(rf/reg-sub
 :trade-patterns
 (fn [db _]
   (sort-by :trade-pattern/name
            (get-in db data.trade-patterns/root))))

(rf/reg-sub
 :trade-pattern-options
 :<- [:trade-patterns]
 (fn [trade-patterns]
   (let [opts (->> trade-patterns
                   (filter (fn [{:keys [trade-pattern/parent-id]
                                 :as   tp}]
                             ;; Only return parent/non-child trade-patterns
                             (when (not parent-id) tp)))
                   (map (fn [{:keys [trade-pattern/id
                                     trade-pattern/name]}]
                          {:label name
                           :value id})))]
     opts)))

;; System

(rf/reg-sub
 :active-view-model     ;; usage: (subscribe [:active-view])
 (fn [db _]             ;; db is the (map) value stored in the app-db atom
   (:active-view-model db)))  ;; extract a value from the application state

(rf/reg-sub
 :loading
 (fn [db [_ key']]
   {key' (true?
          (get-in db [:loading key']))}))
