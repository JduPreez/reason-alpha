(ns reason-alpha.model.common-test
  (:require [reason-alpha.model.common :as sut]
            [clojure.test :refer :all]))

(deftest compute-dependent-fields
  (let [data          [{:id         1
                        :stop-loss  -760
                        :quantity   152
                        :open-price 71.83}
                       {:id         2
                        :stop-loss  -7878
                        :quantity   344
                        :open-price 562}
                       {:id         3
                        :stop-loss  -901
                        :quantity   23
                        :open-price 184.8}
                       {:id         4
                        :stop-loss  -215
                        :quantity   512
                        :open-price 87.11}]
        comps         {:stop-percent-loss
                       {:function
                        "PERCENT(stop-loss/(quantity * open-price))"}
                       :xyz
                       {:require [:stop-percent-loss]
                        :function
                        "100 - IF(stop-percent-loss < 0, stop-percent-loss * -1, stop-percent-loss)"}
                       :multiply-by-2
                       {:require [:stop-percent-loss :xyz]
                        :function
                        "ROUND((xyz + stop-percent-loss) * 2, 2)"}}
        computed-data (sut/compute data {:computations comps})]
    (doseq [{:keys [id xyz multiply-by-2]} computed-data]
      (case id
        1 (do (is (= xyz 93.04))
              (is (= multiply-by-2 172.16)))
        2 (do (is (= xyz 95.93))
              (is (= multiply-by-2 183.72)))
        3 (do
            (is (= xyz 78.8))
            (is (= multiply-by-2 115.2)))
        4 (do
            (is (= xyz 99.52))
            (is (= multiply-by-2 198.08)))))))

(comment
  (clojure.test/run-tests 'reason-alpha.model.common-test)

)
