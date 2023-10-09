(ns reason-alpha.dev-data
  (:require [malli.core :as malli]
            [reason-alpha.data.model :refer [save-all! query disconnect connect]]
            [reason-alpha.data.xtdb :as xtdb]
            [reason-alpha.utils :as utils]))

(malli/=> load-entity-test-data
          [:function
           [:=> :cat :nil]
           [:=> [:cat :string] :nil]])

(defn load-entity-test-data
  ([db]
   (load-entity-test-data db "test_data"))
  ([db test-data-dir]
   (disconnect db)
   (xtdb/drop-db! xtdb/db-name)
   (connect db)
   (doseq [ents (utils/edn-files->clj test-data-dir)]
     (save-all! db ents
                {:role :system}))))

(comment
  (load-entity-test-data)
  (utils/edn-files->clj "test_data")

  (query data.crux/db {:spec '{:find  [n creation-id]
                               :where [[tp :trade-pattern/name n]
                                       [tp :fin-security/creation-id creation-id]]}})

  (require '[crux.api :as c])

  (doseq [ents (utils/edn-files->clj "test_data")]
    (save-all! xtdb/db ents
               {:role :system}))

  (c/q (c/db @xtdb/crux-node)
       '{:find  [nm creation-id]
         :where [[tp :trade-pattern/name nm]
                 [tp :fin-security/creation-id creation-id]]})

  (java.util.UUID/randomUUID)
  )

