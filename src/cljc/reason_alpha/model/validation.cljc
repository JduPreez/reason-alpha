(ns reason-alpha.model.validation
  (:require [malli.core :as m]
            [malli.error :as me]))

(defn valid?
  [schema data]
  (m/validate schema data))

(defn validate
  ([schema data]
   (let [vres  (->> data
                    (m/explain schema)
                    me/humanize
                    (map (fn [exp]
                           (if (vector? exp)
                             (let [[k msgs] exp]
                               [k (map-indexed (fn [idx m]
                                                 (str "(" (inc idx) ") " m))
                                               msgs)])
                             #_else
                             ;; Will overwrite `nil` key, so will only show
                             ;; 1 error for overall data object.
                             [nil exp])))
                    (into {}))
         descr (pr-str vres)]
     (when (seq vres)
       {:type        :failed-validation
        :description descr
        :error       vres})))
  ([schema data member]
   (let [vres (-> schema (validate data) :description member)]
     (when (seq vres)
       {:type        :failed-validation
        :description vres}))))
