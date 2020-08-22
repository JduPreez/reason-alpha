(ns reason-alpha.data-structures
  (:require [traversy.lens :as lens]))

(defn- get-path [item indexed & [path]]
  (let [parent (when-let [p (:parent item)]
                 ((keyword (str p)) indexed))]
    (if parent
      (get-path parent indexed (conj (if path
                                     path
                                     '()) (:id parent)))
      path)))

(defn conj-path [items]
  (let [indexed (->> (map (fn [{:keys [id] :as itm}] ;; Index items into map by ID
                            {(keyword (str id)) itm})
                          items)
                     (apply merge))]
    (map (fn [item]
           (assoc item :path (get-path item indexed)))
         items)))

(comment
  (let [data [{:id 1 :parent 2}
              {:id 2 :parent 4}
              {:id 4}
              {:id 3 :parent 4}]]
    (conj-path data))


  )

