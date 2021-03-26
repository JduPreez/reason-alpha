(ns reason-alpha.env
  (:require
   [selmer.parser :as parser]
   [clojure.tools.logging :as log]
   [reason-alpha.prod-middleware :refer [wrap-prod]]))

(def defaults
  {:init       (fn []
                 (parser/cache-off!)
                 (log/info "\n-=[reason-alpha started successfully using the development profile]=-"))
   :stop       (fn []
                 (log/info "\n-=[reason-alpha has shut down successfully]=-"))
   :middleware wrap-prod})