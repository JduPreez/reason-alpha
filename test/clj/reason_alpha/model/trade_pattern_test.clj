(ns reason-alpha.model.trade-pattern-test
  (:require #_[reason-alpha.model.trade-pattern :as sut]
            [clojure.test :refer :all]))

#_(deftest test-update
  (testing "`update` should create an event to update the name"
    (let [current                   {:trade-pattern/id        (java.util.UUID/randomUUID)
                                     :trade-pattern/name      "Golden Shore"
                                     :trade-pattern/parent-id (java.util.UUID/randomUUID)}
          updates                   {:trade-pattern/name "Breakout"}
          {:keys [cqrs-es/changes]} (sut/update-ent current updates)]
      (is (= 1 (count changes))))))

(comment
  (run-tests 'reason-alpha.model.trade-pattern-test)

  )
