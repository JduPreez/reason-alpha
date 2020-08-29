(ns reason-alpha.data-structures)

(defn- get-ancestors-path
  ([item indexed find-parent path-val]
   (get-ancestors-path item indexed find-parent path-val nil))
  ([item indexed find-parent path-val path]
   (let [parent (when-let [p (find-parent item)]
                  ((keyword (str p)) indexed))]
     (let [pth (or path '())]
       (if (and parent
                (not= item parent)) ;; Check for most basic version of circular ref
         (get-ancestors-path parent indexed find-parent path-val
                   (conj pth (path-val parent)))
         pth)))))

(defn conj-ancestors-path
  "Gets the path of ancestor parents of each item. Doesn't check for any
  circular references. TODO: Should check for circular references"
  [items find-parent path-val]
  (let [indexed (->> (map (fn [{:keys [id] :as itm}] ;; Index items into map by ID
                            {(keyword (str id)) itm})
                          items)
                     (apply merge))]
    (map (fn [item]
           (assoc item
                  :ancestors-path
                  (get-ancestors-path item indexed find-parent path-val)))
         items)))

(comment
  


  )

