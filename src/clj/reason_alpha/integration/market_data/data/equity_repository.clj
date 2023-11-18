(ns reason-alpha.integration.market-data.data.equity-repository
  (:require [reason-alpha.data.model :as data.model]
            [reason-alpha.infrastructure.caching :as caching]
            [reason-alpha.integration.market-data.model.fin-instruments :as fin-instr]
            [reason-alpha.model.core :as model]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.model.validation :as v]
            [reason-alpha.utils :as utils]
            [tick.core :as tick]))

(defn- fix-time
  [{tm :price/time, tp :price/type, :as pr}]
  (if (= :historic tp)
    (assoc pr :price/time (utils/time-at-beginning-of-day tm))
    #_else pr))

(defn save!
  [db prices]
  (clojure.pprint/pprint {::->>>-S prices})
  (if-let [{d   :description
            :as vres} (-> ::fin-instr/prices
                          model/get-def
                          (v/validate prices))]
    (do
      (utils/log ::save! vres)
      (throw (ex-info d vres)))
    #_else (-> fix-time
               (pmap prices)
               (as-> x (data.model/save-all! db x {:role :system})))))

(defn get-prices*
  [db {:keys [type symbol-ticker date-range] :as x}]
  (->> {:spec '{:find  [(pull e [*])]
                :in    [t st [from to]]
                :where [[e :price/id]
                        [e :price/type t]
                        [e :price/symbol-ticker st]
                        [e :price/time tm]
                        [(>= tm from)]
                        [(<= tm to)]]}
        :role :system
        :args [(or type :historic) symbol-ticker date-range]}
       (data.model/query db)
       (map first)))

(def get-prices (caching/wrap get-prices*
                              :fn-cache-key
                              (fn [[_ & {:keys [symbol-ticker date] :as args}]]
                                (println ">>> " symbol-ticker ": " date)
                                symbol-ticker)))

;; TODO: Once working, switch to mem cached function
#_(defn get-prices
  [db & {st :symbol-ticker, ds :dates}]
  (let [ps (->> ds
                (get-prices-per-quarter*
                 :symbol-ticker  st
                 :dates)
                (filter (fn [{:price/keys [time] :as p}]
                          (= time d) p)))]
    ps))

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
