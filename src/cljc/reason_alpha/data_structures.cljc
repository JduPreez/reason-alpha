(ns reason-alpha.data-structures
  (:require [clojure.zip :as z]))

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

(defn hierarchy->nested-maps [items & [{:keys [group-ref-key
                                               id-key sub-items-key]}]]
  (let [group-ref-key (or group-ref-key :parent-id)
        id-key        (or id-key :id)
        sub-items-key (or sub-items-key :sub-items)
        by-parent     (group-by group-ref-key items)]
    (pmap (fn [root]
            (loop [n (z/zipper some?
                               #(by-parent (id-key %))
                               #(assoc %1 sub-items-key (vec %2))
                               root)]
             (if (z/end? n)
               (z/root n)
               (recur (z/next (z/edit n identity))))))
          (by-parent nil))))
