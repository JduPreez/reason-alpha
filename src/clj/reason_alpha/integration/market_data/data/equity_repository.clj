(ns reason-alpha.integration.market-data.data.equity-repository
  (:require [reason-alpha.data.model :as data.model]
            [reason-alpha.integration.market-data.model.fin-instruments :as fin-instr]
            [reason-alpha.model.core :as model]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.infrastructure.caching :as caching]))

(defn save!
  [db share-prices]
  (data.model/save! db share-prices))

(defn- get-share-prices*
  [db & {:keys [symbol dates] :as args}]
  args
  #_(->> {:spec '{:find  [(pull e [*])]
                :where [[e :share-price/account-id account-id]]
                :in    [account-id]}
        :role :system
        :args [account-id]}))

(def get-share-prices (caching/wrap get-share-prices*
                                    :fn-cache-key (fn [[_ & {:keys [symbol dates] :as args}]]
                                                    (println ">>> " symbol ": " dates)
                                                    symbol)))

(comment
  #_(utils/ttl-memoize
     get-share-prices*
     :fn-cache-key (fn [[_ & {:keys [symbol dates] :as args}]]
                     (println ">>> " symbol ": " dates)
                     symbol)
     :ttl 60000)

  (get-share-prices nil
                    :fn-cache-key (fn [args]
                                    (println args)
                                    args)
                    :symbol "ETFT40"
                    :dates [#inst "2023-08-21T00:00:00.000-00:00"
                            #inst "2023-08-20T00:00:00.000-00:00"])

  )
