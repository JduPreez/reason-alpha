(ns reason-alpha.events.models
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.web.api-client :as api-client]
            [reason-alpha.utils :as utils]
            [reason-alpha.events :as events]
            [reason-alpha.data :as data]
            [malli.edn :as medn]
            [malli.core :as m]))

(rf/reg-event-db
 :model.query/getn-response
 (fn [db [_ {:keys [result]}]]
   (let [models                   (get-in db data/models {})
         [_ {:keys [registry]} _] (-> result
                                      medn/read-string
                                      m/form)]
     (assoc-in db data/models (merge models registry)))))

(comment

  (let [edn-str (medn/write-string [:map
                                    [:price/creation-id uuid?]
                                    [:price/id uuid?]
                                    [:price/date inst?]
                                    [:price/open float?]
                                    [:price/close float?]
                                    [:price/high float?]
                                    [:price/low float?]
                                    [:price/adj-close float?]
                                    [:price/volume int?]])]
    (m/form (medn/read-string edn-str))
    )

  )
