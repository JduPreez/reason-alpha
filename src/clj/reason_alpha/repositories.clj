(ns reason-alpha.repositories)

(defprotocol Repository
  (add [repo entity])
  (remove [repo id])
  (get [repo spec])
  (get-by-id [repo id]))