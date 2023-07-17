(ns reason-alpha.model.validation
  (:require [malli.core :as m]
            [malli.error :as me]))

(defn valid?
  [schema data]
  (m/validate schema data))

(defn validate
  ([schema data]
   (->> data
        (m/explain schema)
        me/humanize
        (map (fn [[k msgs]]
               [k (map-indexed (fn [idx m]
                                 (str "(" (inc idx) ") " m))
                               msgs)]))
        (into {})))
  ([schema data member]
   (let [vres (-> schema (validate data) member)]
     (when (seq vres)
       {:type        :failed-validation
        :description vres}))))
