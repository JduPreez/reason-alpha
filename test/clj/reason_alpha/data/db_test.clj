(ns reason-alpha.data.db-test
  (:use midje.sweet)
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

(fact "`db/save!` should convert entity to DB record"
      (let [row (db/upsert! {:security/name          "Facebook"
                             :security/owner-user-id 5
                             :user/user-name         "Frikkie"
                             :user/email             "j@j.com"}
                            (fn [_ _ rw _] rw))]
        (some #(contains? % :security/id) row) => true
        (some #(contains? % :security/owner_user_id) row) => true
        (some #(contains? % :user/id) row) => true
        (some #(contains? % :user/user_name) row) => true))