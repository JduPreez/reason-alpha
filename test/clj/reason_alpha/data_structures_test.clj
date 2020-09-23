(ns reason-alpha.data-structures-test
  (:require [clojure.test :refer :all]
            [reason-alpha.data-structures :as data-structs]))

(def data [{:id 1 :name "One" :parent 2}
           {:id 2 :name "Two" :parent 4}
           {:id 3 :name "Three" :parent 4}
           {:id 4 :name "Four" :parent 5}
           {:id 5 :name "Five"}])

(deftest test-data-structures
  (testing "`data-structures/conj-ancestors-path` should add ancestors path"
    (let [with-ancestors-path (data-structs/conj-ancestors-path
                               data
                               :parent
                               :name
                               :id)]
      (is (every? (fn [{:keys [ancestors-path]}]
                    (some? ancestors-path)) with-ancestors-path))
      (is (= (:ancestors-path (nth with-ancestors-path 0 nil))
             '("Five" "Four" "Two")))
      (is (= (:ancestors-path (nth with-ancestors-path 1 nil))
             '("Five" "Four")))
      (is (= (:ancestors-path (nth with-ancestors-path 2 nil))
             '("Five" "Four")))
      (is (= (:ancestors-path (nth with-ancestors-path 3 nil))
             '("Five")))
      (is (= (:ancestors-path (nth with-ancestors-path 4 nil))
             '()))))
  (testing "`data-structures/conj-ancestors-path` should use specified key"
    (let [with-ancestors-path (data-structs/conj-ancestors-path
                               data
                               :parent
                               :name
                               :id
                               :trade-pattern/ancestors-path)]
      (is (contains? (nth with-ancestors-path 0)
                     :trade-pattern/ancestors-path)))))

(comment
  (clojure.test/run-tests 'reason-alpha.data-structures-test)

)
