(ns reason-alpha.infrastructure.auth-test
  (:require [reason-alpha.infrastructure.auth :as sut]
            [clojure.test :refer :all]
            [reason-alpha.utils :as utils]))

(deftest authorize
  (let [account-id (utils/new-uuid)
        map-ents   (->> (range 1 11)
                        (map
                         (fn [n]
                           [{:position/creation-id         (java.util.UUID/randomUUID)
                             :position/id                  (java.util.UUID/randomUUID)
                             :position/status              (if (even? n) :open :closed)
                             :position/instrument-id       (java.util.UUID/randomUUID)
                             :position/holding-position-id (java.util.UUID/randomUUID)
                             :position/trade-pattern-id    (java.util.UUID/randomUUID)
                             :position/account-id          (if (even? n)
                                                             account-id
                                                             (java.util.UUID/randomUUID))}])))
        tuple-ents (->> (range 1 11)
                        (map
                         (fn [n]
                           (if (even? n)
                             [(utils/new-uuid) account-id]
                             [(utils/new-uuid) (utils/new-uuid)]))))]
    (testing "`authorize` should remove map entities that doesn't belong to `:member`"
      (let [authz-ents (sut/authorize {:fn-get-account (constantly {:account/id account-id})
                                       :crud           [:read]
                                       :role           :member}
                                      map-ents)]
        (is (= (count authz-ents) 5))
        (is (every? (fn [[{aid :position/account-id}]]
                      (= aid account-id)) authz-ents))))

    (testing "`authorize` should remove tuple entities that doesn't belong to `:member`"
      (let [authz-ents (sut/authorize {:fn-get-account (constantly {:account/id account-id})
                                       :crud           [:read]
                                       :role           :member
                                       :id-key         0
                                       :account-id-key 1}
                                      tuple-ents)]
        (is (= (count authz-ents) 5))
        (is (every? (fn [[_ aid]] (= aid account-id)) authz-ents))))

    (testing "`authorize` should only allow `:member` to `:update` own map entities (1)"
      (let [ents-owners (map (fn [[{pid :position/id
                                    aid :position/account-id}]]
                               [pid aid])
                             map-ents)
            authz-ents  (sut/authorize {:fn-get-account  (constantly {:account/id account-id})
                                        :crud            [:update :create]
                                        :role            :member
                                        :entities-owners ents-owners}
                                       map-ents)]
        (is (= (count authz-ents) 5))
        (is (every? (fn [[{aid :position/account-id}]]
                      (= aid account-id)) authz-ents))))

    #_(testing "`authorize` should only allow `:member` to `:update` own map entities (2)"
        (let [aid         #uuid "017f87dc-59d1-7beb-9e1d-7a2a354e5a49"
              ents        [[#:instrument{:id
                                         #uuid "017fb6cc-a299-b5a1-f5db-bf25e5cd3f93",
                                         :creation-id
                                         #uuid "883e2159-7d12-4389-8272-f9137715ebe1",
                                         :name       "i-2",
                                         :symbols
                                         [#:symbol{:ticker "I1",
                                                   :provider
                                                   :yahoo-finance}
                                          #:symbol{:ticker   "i1",
                                                   :provider :saxo-dma}
                                          #:symbol{:ticker "i1",
                                                   :provider
                                                   :easy-equities}],
                                         :type       :share,
                                         :account-id aid}]]
              ents-owners '([#uuid "017fb6cc-a299-b5a1-f5db-bf25e5cd3f93" aid])
              authz-ents  (sut/authorize {:fn-get-account  (constantly {:account/id account-id})
                                          :crud            [:update :create]
                                          :role            :member
                                          :entities-owners ents-owners}
                                         ents)]
        ))))

(comment
  (clojure.test/run-tests 'reason-alpha.infrastructure.auth-test)


  )
