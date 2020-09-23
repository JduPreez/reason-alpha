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
  [items find-parent path-val id-key & [ancestors-path-key]]
  (let [indexed (->> (map (fn [itm] ;; Index items into map by ID
                            {(keyword (str (id-key itm))) itm})
                          items)
                     (apply merge))]
    (println indexed)
    (map (fn [item]
           (assoc item
                  (or ancestors-path-key :ancestors-path)
                  (get-ancestors-path item indexed find-parent path-val)))
         items)))

(comment
  (let [x '(#:trade-pattern{:description    "",
                            :creation-id    nil,
                            :name           "Breakout",
                            :user-id        #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f",
                            :ancestors-path (),
                            :id             #uuid "0174bb78-bc4d-8faa-731f-73a1b2ac2c0f",
                            :parent-id      nil}
            #:trade-pattern{:description    "",
                            :creation-id    nil,
                            :name           "Pullback",
                            :user-id        #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f",
                            :ancestors-path (),
                            :id             #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6",
                            :parent-id      nil}
            #:trade-pattern{:description    "",
                            :creation-id    nil,
                            :name           "Buy Support or Short Resistance",
                            :user-id        #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f",
                            :ancestors-path (),
                            :id             #uuid "3c2d368f-aae5-4d13-a5bd-c5b340f09016",
                            :parent-id      #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6"})]
    #_(->> (map (fn [{:keys [id] :as itm}] ;; Index items into map by ID
                  {(keyword (str id)) itm})
              x)
         (apply merge))
    (conj-ancestors-path x
                         :trade-pattern/parent-id
                         :trade-pattern/name
                         :trade-pattern/id
                         :trade-pattern/ancestors-path))
  )
