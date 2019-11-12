(ns ^{:doc "Security serves as a domain root aggregate."}
 reason-alpha.repositories.securities
  (:require [clojure.java.jdbc :as jdbc]))

#_(def security (reify repos/Repository
                (add [_ entity]
                  entity)
                (remove [_ id]
                  (new Object))
                (get [_ spec]
                  [])
                (get-by-id [_ id]
                  (new Object))))