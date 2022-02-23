(ns reason-alpha.services.model
  (:require [reason-alpha.model.core :as mdl]))

(defn getn [fn-get-ctx model-ks]
  (let [{:keys [send-message]} (fn-get-ctx model-ks)
        models                 (mdl/get-defs model-ks)]
    (send-message
     [:model.query/getn-response {:result models
                                  :type   :success}])))
