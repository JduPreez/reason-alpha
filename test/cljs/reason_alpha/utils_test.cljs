(ns reason-alpha.utils-test
  (:require [cljs.test :as t :refer-macros [deftest is testing run-tests]]
            [reason-alpha.utils :as utils]))

(deftest test-kw-keys
  (testing "'kw-keys' should change map with string keys to one with keyword keys"
    (let [data (utils/kw-keys {"some/str-key"    0
                               "another/str-key" "yup, it's true"})
          key' (first (keys data))]
      (is (keyword? key')))))

(deftest test-merge-by-id
  (testing "'merge-by-id' should use creation-id to merge"
    (let [data (utils/merge-by-id :trade-pattern
                                  [{:trade-pattern/creation-id 123
                                    :trade-pattern/name        "First Name"}
                                   {:trade-pattern/creation-id 356
                                    :trade-pattern/name        "blah"}]
                                  [{:trade-pattern/creation-id 123
                                    :trade-pattern/name        "2nd Name"}
                                   {:trade-pattern/creation-id 789
                                    :trade-pattern/name        "uchedy smackedy"}])]
      (is (= 3 (count data)))))
  (testing "'merge-by-id' should use id to merge"
    (let [data (utils/merge-by-id :trade-pattern
                                  [{:trade-pattern/id 123
                                    :trade-pattern/name        "First Name"}
                                   {:trade-pattern/id 356
                                    :trade-pattern/name        "blah"}]
                                  [{:trade-pattern/id 123
                                    :trade-pattern/name        "2nd Name"}
                                   {:trade-pattern/id 789
                                    :trade-pattern/name        "uchedy smackedy"}])]
      (is (= 3 (count data))))))

(comment
  (run-tests 'reason-alpha.utils-test)

  )
