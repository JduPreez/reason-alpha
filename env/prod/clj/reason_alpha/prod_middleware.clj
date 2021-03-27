(ns reason-alpha.prod-middleware
  (:require
   [ring.middleware.reload :refer [wrap-reload]]
   [selmer.middleware :refer [wrap-error-page]]
   [prone.middleware :refer [wrap-exceptions]]))

(defn wrap-prod [handler]
  (-> handler
      wrap-reload
      wrap-error-page
      (wrap-exceptions {:app-namespaces ['reason-alpha]})))
