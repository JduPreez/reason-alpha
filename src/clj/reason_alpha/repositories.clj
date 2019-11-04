(ns reason-alpha.repositories)

(defn to-entity [query-result]
  nil)
#_(-> [[:security/name          := "Donnie Brasco"]]
     [:and
      [:security/owner-user-id :< 898]]
    (to-sql)
    (to-entity))

(defprotocol Repository
  (add'       [repo entity])  
  (remove'    [repo id])  
  (get'       [repo spec])  
  (get-by-id' [repo id]))
