(ns reason-alpha.web.service-api-test
  (:require [cljs.test :as t :refer-macros [deftest is testing async]]
            [reason-alpha.web.service-api :as svc-api]))

(deftest test-async-awesome
  (testing "the API is awesome"
    (async done
           (svc-api/trade-patterns (fn [res]
                                     (is (some? res))
                                     (done))))))