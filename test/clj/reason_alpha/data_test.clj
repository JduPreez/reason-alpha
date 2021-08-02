(ns reason-alpha.data-test
  (:import [com.github.f4b6a3.uuid UuidCreator])
  (:require [clojure.test :refer :all]
            [reason-alpha.data :as data :refer [db save!]]))

(deftest test-data
  (testing "`data/to-sql` converts 1 condition specification '='"
    (is (= (#'data/to-sql :select
                          [[:security/name := "Hugo Boss"]])
           ["SELECT 'security' reason_alpha_table, * FROM \"REASON-ALPHA\".security WHERE \"REASON-ALPHA\".security.name = ?"
            "Hugo Boss"])))

  (testing "`data/to-sql` converts 2 condition specification 'OR'"
    (is (= (#'data/to-sql :select
                          [[:security/name := "Facebook"]
                           [:or :security/owner-user-id := 4]])
           ["SELECT 'security' reason_alpha_table, * FROM \"REASON-ALPHA\".security WHERE \"REASON-ALPHA\".security.name = ? OR \"REASON-ALPHA\".security.owner_user_id = ?"
            "Facebook"
            4])))

  (testing "`data/to-sql` converts `:delete` to `DELETE` SQL"
    (is (= (#'data/to-sql :delete
                          [[:security/name := "Facebook"]])
           ["DELETE FROM \"REASON-ALPHA\".security WHERE \"REASON-ALPHA\".security.name = ?"
            "Facebook"])))

  (testing "`data/row->entity` should map from table to entity schema"
    (is (= (#'data/row->entity {:id                 2
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
                      (fn [{row :row}] row))]
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
          rows   (save! db entity (fn [{row :row}] row))]
      (is (= (some #(= (:security/id %) (:security/id entity)) rows) true))
      (is (= (some #(= (:user/id %) (:user/id entity)) rows) true)))))

(deftest test-add-all
  (testing "`entities->add-all-cmd` should not overwrite id"
    (let [id1            #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6"
          id2            #uuid "3c2d368f-aae5-4d13-a5bd-c5b340f09016"
          entities       [{:trade-pattern/id          id1
                           :trade-pattern/name        "Pullback"
                           :trade-pattern/description ""
                           :trade-pattern/parent-id   nil
                           :trade-pattern/user-id     #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"}
                          {:trade-pattern/id          id2
                           :trade-pattern/name        "Buy Support or Short Resistance"
                           :trade-pattern/description ""
                           :trade-pattern/parent-id   #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6"
                           :trade-pattern/user-id     #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"}
                          {:trade-pattern/name        "Just another test"
                           :trade-pattern/description ""
                           :trade-pattern/parent-id   #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6"
                           :trade-pattern/user-id     #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"}]
          {:keys [rows]} (#'data/entities->add-all-cmd entities)]
      (is (some (fn [{:keys [id]}]
                  (= id id1)) rows))
      (is (some (fn [{:keys [id]}]
                  (= id id2)) rows)))))

;; TODO: Add test for (to-query [[:security/*]]) 

(comment
  (let [id1            #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6"
        id2            #uuid "3c2d368f-aae5-4d13-a5bd-c5b340f09016"
        entities       [{:trade-pattern/id          id1
                         :trade-pattern/name        "Pullback"
                         :trade-pattern/description ""
                         :trade-pattern/parent-id   nil
                         :trade-pattern/user-id     #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"}
                        {:trade-pattern/id          id2
                         :trade-pattern/name        "Buy Support or Short Resistance"
                         :trade-pattern/description ""
                         :trade-pattern/parent-id   #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6"
                         :trade-pattern/user-id     #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"}]
        {:keys [rows]} (#'data/entities->add-all-cmd entities)]
    (some (fn [{:keys [id]}]
               (= id id1)) rows))

  (clojure.test/run-tests 'reason-alpha.data-test)

  )
