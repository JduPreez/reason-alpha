(ns ^{:doc "Security serves as a domain root aggregate."}
 reason-alpha.repositories.securities
  (:require [reason-alpha.repositories :as repos]))

(def securityRepository (reify repos/Repository
                          (add [_ entity]
                            entity)
                          (remove [_ id]
                            (new Object))
                          (get [_ spec]
                            [])
                          (get-by-id [_ id]
                            (new Object))))