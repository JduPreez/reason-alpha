(ns user
  (:require [reason-alpha.core :as reason-alpha]
            [reason-alpha.dev-data :as dev-data]
            [reason-alpha.infrastructure.server :as server]
            [reason-alpha.model :as model]
            [shadow.cljs.devtools.api :as shadow]))

(defn start
  ([load-test-data?]
   (reason-alpha/start)
   (when load-test-data?
     (let [{:keys [db-name]} model/db-conf]
       (dev-data/load-entity-test-data (::model/db @model/*system)
                                       db-name))))
  ([]
   (reason-alpha/start)))

(defn stop []
  (reason-alpha/stop))

(comment
  (dev-data/load-entity-test-data (::model/db @model/*system))

  (start true)

  (start)

  @model/*system

  (stop)

  (shadow/compile :app)

)
