(ns reason-alpha.web.service-api-test
  (:require [cljs.test :as t :refer-macros [deftest is testing async run-tests]]
            [reason-alpha.web.service-api :as svc-api]))

#_(deftest test-async-awesome
  (testing "the API is awesome"
    (async done
           (svc-api/trade-patterns (fn [res]
                                     (is (some? res))
                                     (done))))))
(deftest test-entities->commands
  (testing "'entities->commands' should create a http-request for each rentity"
    (let [cmds (svc-api/entities->commands [[:trade-pattern {:trade-pattern/id            1
                                                             :trade-pattern/name          "Facebook"
                                                             :trade-pattern/owner-user-id 5
                                                             :user/user-name              "Frikkie"
                                                             :user/email                  "j@j.com"}]
                                            [:trade-pattern {:trade-pattern/name          "Facebook"
                                                             :trade-pattern/owner-user-id 5
                                                             :user/user-name              "Frikkie"
                                                             :user/email                  "j@j.com"}]])]
      (is (= (count cmds) 2))
      (is (= (some (fn [_ {:keys [method]}] (= method :put)) cmds)))
      (is (= (some (fn [_ {:keys [method]}] (= method :post)) cmds))))))

(comment
  (run-tests 'reason-alpha.web.service-api-test)
)
