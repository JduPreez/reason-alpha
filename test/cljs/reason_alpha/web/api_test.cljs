(ns reason-alpha.web.api-test
  (:require [ajax.core :as ajax]
            [cljs.test :as t :refer-macros [deftest is testing]]))

#_(deftest test-async-awesome
  (testing "the API is awesome"
    (let [url "http://foo.com/api.edn"
          res (http/get url)]
      (async done
             (go
               (is (= (<! res) :awesome))
               (done))))))