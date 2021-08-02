(ns reason-alpha.data)

(def ^:const selected [:selected])

(def ^:const active-view-model [:active-view-model])

(def ^:const api-info [:data :api])

(defn entity-data [entities-type]
  [:data entities-type])
