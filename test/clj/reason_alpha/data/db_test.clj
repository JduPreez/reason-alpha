(ns reason-alpha.data.db-test
  (:use midje.sweet)
  (:import [com.github.f4b6a3.uuid UuidCreator])
  (:require [clojure.string :as string]
            [reason-alpha.data.db :as db]))

(fact "`db/to-query` converts 1 condition specification '='"
      (#'db/to-query [[:security/name := "Hugo Boss"]]) =>
      ["SELECT 'security' reason_alpha_table, * FROM \"REASON-ALPHA\".security WHERE \"REASON-ALPHA\".security.name = ?"
       "Hugo Boss"])

(fact "`db/to-query` converts 2 condition specification 'OR'"
      (#'db/to-query [[:security/name := "Facebook"]
                    [:or :security/owner-user-id := 4]]) =>
      ["SELECT 'security' reason_alpha_table, * FROM \"REASON-ALPHA\".security WHERE \"REASON-ALPHA\".security.name = ? OR \"REASON-ALPHA\".security.owner_user_id = ?"
       "Facebook"
       4])

(fact "`db/to-entity` should map from table to entity schema"
      (#'db/to-entity {:id                 2
                       :name               "Facebook"
                       :owner_user_id      345
                       :reason_alpha_table "security"}) => {:security/id            2
                                                            :security/name          "Facebook"
                                                            :security/owner-user-id 345})

(fact "`db/save!` should convert entity to DB insert record"
      (let [rows (db/save! {:security/name          "Facebook"
                            :security/owner-user-id 5
                            :user/user-name         "Frikkie"
                            :user/email             "j@j.com"}
                            (fn [_ _ rw _] rw))]
        (some #(contains? % :security/id) rows) => true
        (some #(contains? % :security/owner_user_id) rows) => true
        (some #(contains? % :user/id) rows) => true
        (some #(contains? % :user/user_name) rows) => true))

(fact "`db/save!` should convert entity to DB update record"
      (let [entity {:security/id            (UuidCreator/getLexicalOrderGuid)
                    :security/name          "Facebook"
                    :security/owner-user-id 5
                    :user/id                (UuidCreator/getLexicalOrderGuid)
                    :user/user-name         "Frikkie"
                    :user/email             "j@j.com"}
            rows   (db/save! entity (fn [_ _ rw _] rw))]
        (some #(= (:security/id %) (:security/id entity)) rows) => true
        (some #(= (:user/id %) (:user/id entity)) rows) => true))