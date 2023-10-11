(ns reason-alpha.integration.market-data.data.equity-repository
  (:require [reason-alpha.data.model :as data.model]
            [reason-alpha.infrastructure.caching :as caching]
            [reason-alpha.integration.market-data.model.fin-instruments :as fin-instr]
            [reason-alpha.model.core :as model]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.utils :as utils]
            [tick.core :as tick]))

(defn- fix-time [{t :price/time :as p}]
  (let [year (-> t tick/year str)]
    (assoc p
           :price/time         (-> t
                                   tick/date
                                   tick/beginning
                                   (tick/in "UTC")
                                   tick/inst)
           :price/year-quarter (-> t
                                   tick/date
                                   tick/month
                                   tick/int
                                   (/ 3)
                                   utils/round-up
                                   int
                                   (as-> q (str year "-Q" q))))))

(defn save!
  [db prices]
  (->> prices
       (map fix-time)
       (data.model/save-all! db)))

(defn get-historic-prices-per-quarter*
  [db & {:keys [symbol dates] :as args}]
  (let [yr-quarters (->> dates
                         (map #(-> {:price/time %} fix-time :price/year-quarter))
                         distinct)]
    ;; Now fetch all prices for the symbol and where the `:price/year-quarter` match
    #_(->> {:spec '{:find  [(pull e [*])]
                    :where [[e :price/account-id account-id]]
                    :in    [account-id]}
            :role :system
            :args [account-id]})
    nil))

(def get-historic-prices-per-quarter (caching/wrap get-historic-prices-per-quarter*
                                                   :fn-cache-key
                                                   (fn [[_ & {:keys [symbol dates] :as args}]]
                                                     (println ">>> " symbol ": " dates)
                                                     symbol)))

(comment
  (-> #inst "2023-08-21T12:12:00.000-00:00"
      tick/date
      tick/beginning
      (tick/in "UTC")
      tick/inst
      #_(tick/at "00:00"))

  (get-historic-prices-per-quarter nil
                                   :fn-cache-key (fn [args]
                                                   (println args)
                                                   args)
                                   :symbol "ETFT40"
                                   :dates [#inst "2023-04-21T03:00:00.000-00:00"
                                           #inst "2023-08-20T14:20:00.000-00:00"])

  )
