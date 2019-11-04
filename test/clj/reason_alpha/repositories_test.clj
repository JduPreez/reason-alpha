(ns reason-alpha.repositories-test
  (:use midje.sweet)
  (:require [clojure.string :as string]
            [reason-alpha.repositories :as repos]))

(fact "`repositories.to-sql` converts specification '=' x1"
      (repos/to-sql [[:security/name := "Recursive Enigma"]]) => "SELECT * FROM REASON_ALPHA.security WHERE REASON_ALPHA.security.name = ?")

(fact "`repositories.to-sql` converts specification 'OR' x2"
      (repos/to-sql [[:security/name := "Recursive Enigma"]
                     [:or :security/owner-user-id := 4]]) => "SELECT * FROM REASON_ALPHA.security WHERE REASON_ALPHA.security.name = ? (OR REASON_ALPHA.security.owner-user-id = ?)")