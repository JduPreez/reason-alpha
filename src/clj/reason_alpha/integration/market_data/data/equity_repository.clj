(ns reason-alpha.integration.market-data.data.equity-repository
  (:require [reason-alpha.data.model :as data.model]
            [reason-alpha.infrastructure.caching :as caching]
            [reason-alpha.integration.market-data.model.fin-instruments :as fin-instr]
            [reason-alpha.model.core :as model]
            [reason-alpha.model.mapping :as mapping]
            [reason-alpha.utils :as utils]
            [tick.core :as tick]))

(defn- fix-time [{t :price/time :as p}]
  (assoc p
         :price/time (utils/time-at-beginning-of-day t)))

(defn save!
  [db prices]
  (->> prices
       (map fix-time)
       (data.model/save-all! db)))

(defn get-prices*
  [db & {:keys [type symbol-ticker date-range] :as args}]
  ;; Now fetch all prices for the symbol and where the `:price/year-quarter` match
  (->> {:spec '{:find  [(pull e [*])]
                :in    [t st [from to]]
                :where [[e :price/type t]
                        [e :price/symbol-ticker st]
                        [e :price/time tm]
                        [(>= tm from)]
                        [(<= tm to)]]}
        :role :system
        :args [(or type :historic) symbol-ticker date-range]}
       (data.model/query db)))

(def get-prices (caching/wrap get-prices*
                              :fn-cache-key
                              (fn [[_ & {:keys [symbol-ticker date] :as args}]]
                                (println ">>> " symbol-ticker ": " dates)
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
