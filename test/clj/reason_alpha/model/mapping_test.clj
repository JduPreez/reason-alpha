(ns reason-alpha.model.mapping-test
  (:require  [clojure.test :refer :all]
             [reason-alpha.model.mapping :as mapping]
             [reason-alpha.utils :as utils]))

(def currency-acc-path [:instrument/currency-instrument :instrument/account :account/user-name])

(def query-dao-model [:map
                      [:instrument-id
                       {:optional true, :command-path [:instrument/id]}
                       uuid?]
                      [:instrument-creation-id
                       {:command-path [:instrument/creation-id]}
                       uuid?]
                      [:instrument-name
                       {:title        "Instrument",
                        :optional     true,
                        :command-path [:instrument/name]}
                       string?]
                      [:instrument-type
                       {:title        "Type",
                        :optional     true,
                        :ref          :instrument/type,
                        :command-path [:instrument/type]}
                       keyword?]
                      [:yahoo-finance
                       {:title        "Yahoo! Finance",
                        :optional     true,
                        :pivot        :symbol/provider,
                        :command-path [:instrument/symbols 0 :symbol/ticker]}
                       string?]
                      [:saxo-dma
                       {:title        "Saxo/DMA",
                        :optional     true,
                        :pivot        :symbol/provider,
                        :command-path [:instrument/symbols 0 :symbol/ticker]}
                       string?]
                      [:currency-account
                       {:title        "Currency Account"
                        :optional     true
                        :command-path currency-acc-path}]])

(deftest mapping-query-dao->command-ent
  (let [{:keys [instrument-id
                instrument-creation-id
                instrument-type
                instrument-name
                saxo-dma
                yahoo-finance
                currency-account]
         :as   query-dao} {:instrument-id          (utils/new-uuid)
                           :instrument-creation-id (utils/new-uuid)
                           :instrument-type        :share
                           :instrument-name        "Tencent"
                           :saxo-dma               "00700:xhkg"
                           :yahoo-finance          "0700.hk"
                           :currency-account       "ark@ark.com"}
        {:keys [instrument/creation-id
                instrument/id
                instrument/type
                instrument/name
                instrument/symbols]
         :as   cmd-ent}   (mapping/query-dao->command-ent query-dao-model query-dao)]
    (testing "`query-dao->command-ent` should map non-nested path"
      (is (= id instrument-id))
      (is (= creation-id instrument-creation-id))
      (is (= type instrument-type))
      (is (= name instrument-name)))
    (testing "`query-dao->command-ent` should map nested path without vec"
      (is (= currency-account (get-in cmd-ent currency-acc-path))))
    (testing "`query-dao->command-ent` should map nested path with vec"
      (is (= saxo-dma (some
                       (fn [{:keys [symbol/ticker
                                    symbol/provider]}]
                         (when (= provider :saxo-dma)
                           ticker)) symbols)))
      (is (= yahoo-finance (some
                            (fn [{:keys [symbol/ticker
                                         symbol/provider]}]
                              (when (= provider :yahoo-finance)
                                ticker)) symbols))))))

(comment
  (clojure.test/run-tests 'reason-alpha.model.mapping-test)

  )
