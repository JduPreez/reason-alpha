(ns reason-alpha.services.model-service
  (:require [malli.edn :as medn]
            [reason-alpha.model.core :as mdl]))

(defn getn [fn-get-ctx model-ks]
  (let [{:keys [send-message]} (fn-get-ctx)
        models                 (mdl/get-defs model-ks)
        malli-edn              (medn/write-string [:schema {:registry models}
                                                   (first model-ks)])]
    (send-message
     [:model.query/getn-result {:result malli-edn
                                :type   :success}])))


(comment

  (require '[reason-alpha.model.fin-instruments :as finstr]
           '[clojure.edn :as edn])

  (prn-str finstr/Instrument)

  (let [ks      [:model/instrument :model/symbol]
        models  (mdl/get-defs ks)
        edn-str (medn/write-string [:schema {:registry models} (first ks)])]
    (medn/read-string edn-str))


  )
