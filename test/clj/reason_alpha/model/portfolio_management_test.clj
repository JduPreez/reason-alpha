(ns reason-alpha.model.portfolio-management-test
  (:require [clojure.test :refer :all]
            [reason-alpha.model.portfolio-management :as sut]
            [reason-alpha.utils :as utils]))

(deftest test-stop-loss-amount
  (testing (str "`stop-loss-amount` should calculate correctly for empty holding position"
                "with sub-positions with partial stops")
    (let [open-price     (rand 100)
          holding-pos-id (utils/new-uuid)
          holding-pos    {:position-creation-id (utils/new-uuid)
                          :position-id          holding-pos-id
                          :long-short           :long}
          sub-positions  [{:position-creation-id (utils/new-uuid)
                           :position-id          (utils/new-uuid)
                           :holding-position-id  holding-pos-id
                           :long-short           :long
                           :quantity             35.0
                           :open-price           73.77
                           :stop                 68.5}
                          {:position-creation-id (utils/new-uuid)
                           :position-id          (utils/new-uuid)
                           :holding-position-id  holding-pos-id
                           :long-short           :long
                           :quantity             35.0
                           :open-price           61.04}
                          {:position-creation-id (utils/new-uuid)
                           :position-id          (utils/new-uuid)
                           :holding-position-id  holding-pos-id
                           :long-short           :long
                           :quantity             30.0
                           :open-price           78.25
                           :stop                 70.0}
                          {:position-creation-id (utils/new-uuid)
                           :position-id          (utils/new-uuid)
                           :holding-position-id  holding-pos-id
                           :long-short           :long
                           :quantity             30.0
                           :open-price           57.84}]
          {:keys [open-price stop
                  stop-loss-amount]
           :as   pos}    (sut/stop-loss-amount holding-pos sub-positions)]
      (is (= (float 67.70) open-price))
      (is (= (float 34.60) stop))
      (is (= (float -4303.55) stop-loss-amount)))))

(comment
  (clojure.test/run-tests 'reason-alpha.model.portfolio-management-test)

  (class 30.4)
  

  )
