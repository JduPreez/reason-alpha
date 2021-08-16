(ns reason-alpha.model
  (:require [traversy.lens :as tl]))

(defn entity-type [entity-map]
  (-> 
      (tl/view-single (tl/*>
                       tl/all-keys
                       (tl/conditionally #(= (name %) "id"))))))

(comment

  
  

  )
