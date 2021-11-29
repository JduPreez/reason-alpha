(ns reason-alpha.data-structures)

(defn- get-ancestors-path
  ([item indexed find-parent path-val]
   (get-ancestors-path item indexed find-parent path-val nil))
  ([item indexed find-parent path-val path]
   (let [parent (when-let [p (find-parent item)]
                  ((keyword (str p)) indexed))
         pth    (or path (list (path-val item)))]
     (if (and parent
              (not= item parent)) ;; Check for most basic version of circular ref
       (get-ancestors-path parent indexed find-parent path-val
                           (conj pth (path-val parent)))
       pth))))

(defn conj-ancestors-path
  "Gets the path of ancestor parents of each item. Doesn't check for any
  circular references. TODO: Should check for circular references"
  [items find-parent path-val id-key & [ancestors-path-key]]
  (let [indexed (->> (map (fn [itm] ;; Index items into map by ID
                            {(keyword (str (id-key itm))) itm})
                          items)
                     (apply merge))]
    (mapv (fn [item]
           (assoc item
                  (or ancestors-path-key :ancestors-path)
                  (get-ancestors-path item indexed find-parent path-val)))
         items)))

(comment
  (let [x '({:crux.db/id                   #uuid "0174bb78-bc4d-8faa-731f-73a1b2ac2c0f"
             :trade-pattern/description    "",
             :trade-pattern/creation-id    nil,
             :trade-pattern/name           "Breakout",
             :trade-pattern/user-id        #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f",
             :trade-pattern/ancestors-path (),
             :trade-pattern/id             #uuid "0174bb78-bc4d-8faa-731f-73a1b2ac2c0f",
             :trade-pattern/parent-id      nil}
            {:trade-pattern/description    "",
             :trade-pattern/creation-id    nil,
             :trade-pattern/name           "Pullback",
             :trade-pattern/user-id        #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f",
             :trade-pattern/ancestors-path (),
             :trade-pattern/id             #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6",
             :crux.db/id                   #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6",
             :trade-pattern/parent-id      nil}
            { :trade-pattern/description   "",
             :trade-pattern/creation-id    nil,
             :trade-pattern/name           "Buy Support or Short Resistance",
             :trade-pattern/user-id        #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f",
             :trade-pattern/ancestors-path (),
             :trade-pattern/id             #uuid "3c2d368f-aae5-4d13-a5bd-c5b340f09016",
             :crux.db/id                   #uuid "3c2d368f-aae5-4d13-a5bd-c5b340f09016",
             :trade-pattern/parent-id      #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6"})]
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
