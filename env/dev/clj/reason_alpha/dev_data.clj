(ns reason-alpha.dev-data
  (:require [reason-alpha.data :refer [add-all! db]]
            [reason-alpha.data.migrations :as migrations]
            [reason-alpha.utils :as utils]))

; Generate UUID (java.util.UUID/randomUUID)

(defn load-entity-test-data
  ([migrate]
   (load-entity-test-data migrate "test_data"))
  ([migrate test-data-dir]
   (when migrate (migrations/run))
   (doseq [ents (utils/edn-files->clj test-data-dir)]
     (add-all! db ents))))

(comment
  (load-entity-test-data true))

