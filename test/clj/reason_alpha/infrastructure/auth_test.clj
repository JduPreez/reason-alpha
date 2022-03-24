(ns reason-alpha.infrastructure.auth-test
  (:require [reason-alpha.infrastructure.auth :as sut]
            [clojure.test :refer :all]
            [reason-alpha.utils :as utils]))

(deftest authorize
  (let [account-id (utils/new-uuid)]
    (testing "`authorize` should remove map entities that doesn't belong to `:member`"
      (let [ents       (->> (range 1 11)
                            (map
                             (fn [n]
                               {:position/creation-id         (java.util.UUID/randomUUID)
                                :position/id                  (java.util.UUID/randomUUID)
                                :position/status              (if (even? n) :open :closed)
                                :position/instrument-id       (java.util.UUID/randomUUID)
                                :position/holding-position-id (java.util.UUID/randomUUID)
                                :position/trade-pattern-id    (java.util.UUID/randomUUID)
                                :position/account-id          (if (even? n)
                                                                account-id
                                                                (java.util.UUID/randomUUID))})))
            authz-ents (sut/authorize {:fn-get-account (constantly {:account/id account-id})
                                       :crud           :read
                                       :role           :member
                                       :entities       ents})]
        (is (= (count authz-ents) 5))
        (is (every? (fn [{aid :position/account-id}]
                      (= aid account-id)) authz-ents))))
    (testing "`authorize` should remove tuple entities that doesn't belong to `:member`"
      (let [ents       (->> (range 1 11)
                            (map
                             (fn [n]
                               (if (even? n)
                                 [(utils/new-uuid) account-id]
                                 [(utils/new-uuid) (utils/new-uuid)]))))
            authz-ents (sut/authorize {:fn-get-account (constantly {:account/id account-id})
                                       :crud           :read
                                       :role           :member
                                       :entities       ents
                                       :account-id-key 1})]
        (is (= (count authz-ents) 5))
        (is (every? (fn [[_ aid]] (= aid account-id)) authz-ents))))))

(comment
  (clojure.test/run-tests 'reason-alpha.infrastructure.auth-test)


  )
