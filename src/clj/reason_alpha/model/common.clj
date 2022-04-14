(ns reason-alpha.model.common
  (:require [malli.core :as m]
            [reason-alpha.model.core :as model :refer [def-model]]
            [reason-alpha.model.accounts :as accounts]))

(def ^:dynamic *context* {})

(defn result-schema [result-schema]
  [:map
   [:result {:optional true} result-schema]
   [:type [:enum :error :success :warn :info]]
   [:error {:optional true} any?]
   [:description {:optional true} string?]
   [:nr-items {:optional true} int?]])

(def-model getContext
  :model/get-context
  [:=> :cat
       [:map
        [:send-message :any]
        [:*connected-users :any]
        [:user-account accounts/Account]]])

(m/=> get-context getContext)

(defn get-context []
  *context*)

#_(defn get-context [data]
  (when data
    (-> data
        meta
        :context)))

#_(defn set-context [data ctx]
  (if (and data (instance? clojure.lang.IObj
                           data))
    (with-meta data {:context ctx})
    data))

(comment

  (let [d (set-context [:one :two] {:something "very cool"})]
    (get-context d))
  

  )
