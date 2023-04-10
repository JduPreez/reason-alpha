(ns reason-alpha.subs.account-profile
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :account-profile
 [_]
 {:account-currency                 "9090909"
  :subscription-eod-historical-data "999"})
