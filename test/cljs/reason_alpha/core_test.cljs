(ns reason-alpha.core-test
  (:require [cljs.test :as t :refer-macros [deftest is testing]]))

(deftest test-home
  (testing "Hello World!"
    (is (= true true))))