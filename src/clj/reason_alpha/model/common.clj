(ns reason-alpha.model.common
  (:require [reason-alpha.model.core :as model :refer [def-model]]))

(defn result-schema [result-schema]
  [:map
   [:result {:optional true} result-schema]
   [:type [:enum :error :success :warn :info]]
   [:error {:optional true} any?]
   [:description {:optional true} string?]
   [:nr-items {:optional true} int?]])

(defn get-context [data]
  (when data
    (-> data
        meta
        :context)))

(defn set-context [data ctx]
  (if (and data (instance? clojure.lang.IObj
                           data))
    (with-meta data ctx)
    data))
