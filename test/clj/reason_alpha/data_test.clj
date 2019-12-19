(ns reason-alpha.data-test
  (:import [com.github.f4b6a3.uuid UuidCreator])
  (:require [clojure.test :refer :all]
            [reason-alpha.data :as data :refer [db save!]]))

(deftest test-data
  (testing "`data/to-query` converts 1 condition specification '='"
    (is (= (#'data/to-query [[:security/name := "Hugo Boss"]])
           ["SELECT 'security' reason_alpha_table, * FROM \"REASON-ALPHA\".security WHERE \"REASON-ALPHA\".security.name = ?"
            "Hugo Boss"])))

  (testing "`data/to-query` converts 2 condition specification 'OR'"
    (is (= (#'data/to-query [[:security/name := "Facebook"]
                             [:or :security/owner-user-id := 4]])
           ["SELECT 'security' reason_alpha_table, * FROM \"REASON-ALPHA\".security WHERE \"REASON-ALPHA\".security.name = ? OR \"REASON-ALPHA\".security.owner_user_id = ?"
            "Facebook"
            4])))
  
  (testing "`data/to-entity` should map from table to entity schema"
    (is (= (#'data/to-entity {:id                 2
                              :name               "Facebook"
                              :owner_user_id      345
                              :reason_alpha_table "security"})
           {:security/id            2
            :security/name          "Facebook"
            :security/owner-user-id 345})))
  
  (testing "`save!` should convert entity to DB insert record"
    (let [rows (save! db {:security/name          "Facebook"
                          :security/owner-user-id 5
                          :user/user-name         "Frikkie"
                          :user/email             "j@j.com"}
                      (fn [_ _ rw _] rw))]
      (is (= (some #(contains? % :security/id) rows) true))
      (is (= (some #(contains? % :security/owner_user_id) rows) true))
      (is (= (some #(contains? % :user/id) rows) true))
      (is (= (some #(contains? % :user/user_name) rows) true))))
  
  (testing "`save!` should convert entity to DB update record"
    (let [entity {:security/id            (UuidCreator/getLexicalOrderGuid)
                  :security/name          "Facebook"
                  :security/owner-user-id 5
                  :user/id                (UuidCreator/getLexicalOrderGuid)
                  :user/user-name         "Frikkie"
                  :user/email             "j@j.com"}
          rows   (save! db entity (fn [_ _ rw _] rw))]
      (is (= (some #(= (:security/id %) (:security/id entity)) rows) true))
      (is (= (some #(= (:user/id %) (:user/id entity)) rows) true)))))