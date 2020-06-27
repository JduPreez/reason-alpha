(ns reason-alpha.subscriptions
  (:require [re-frame.core :as rf]
            [reason-alpha.utils :as utils]))

(rf/reg-sub
 :trade-patterns
 (fn [db _]
   (utils/log ::trade-patterns (get-in db [:data :trade-pattern]))
   (utils/str-keys (sort-by :trade-pattern/name
                            (get-in db [:data :trade-pattern])))))
