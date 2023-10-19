(ns reason-alpha.integration.market-data.integration.marketstack-api-client
  (:require [ajax.core :as ajax :refer [GET]]
            [clojure.core.async
             :as as
             :refer [>! <! >!! <!! go chan buffer close!
                     alts! go-loop]]
            [clojure.string :as str]
            [outpace.config :refer [defconfig]]
            [reason-alpha.infrastructure.caching :as caching]
            [reason-alpha.utils :as utils]
            [tick.core :as tick]))

(def base-url "http://api.marketstack.com/v1/")

(def eod-url "eod/latest")

(def *cache (caching/ttl-cache))

;; Only used for testing
(defconfig dev-access-key)

(defn cache-key
  [symbol]
  (str (tick/today) "-" symbol))

(defn- build-idx-symbol->s-inf
  [symbols]
  (->> symbols
       (map (fn [{:keys [symbol] :as s}]
              [symbol s]))
       (into {})))

(defn handler
  [result-out-chnl symbols {:keys [data] :as r}]
  (let [idx-symbol->s-inf (build-idx-symbol->s-inf symbols)]
    (go-loop [d data]
      (when-let [{:keys [symbol open close high low date volume]} (peek d)]
        (let [p     {:price-id                (utils/new-uuid)
                     :price-creation-id       (utils/new-uuid)
                     :symbol-ticker           symbol
                     :symbol-provider         :marketstack
                     #_#_:holding-id          (get idx-hid-tkrs code)
                     :price-time              date
                     :price-open              open
                     :price-close             close
                     :price-high              high
                     :price-low               low
                     #_#_price-previous-close previousClose
                     :price-volume            volume
                     #_#_:price-change        change}
              s-inf (get idx-symbol->s-inf symbol)
              p     (merge s-inf p)]
          (>! result-out-chnl p)
          (caching/set-cache-item *cache (cache-key symbol) p)
          (recur (pop d)))))))

(defn err-handler
  [result-out-chnl symbols {:keys [status status-text] :as r}]
  (let [idx-symbol->s-inf (build-idx-symbol->s-inf symbols)]
    (go-loop [syms symbols]
      (when-let [s (peek syms)]
        (do
          (>! result-out-chnl (assoc (get idx-symbol->s-inf s)
                                     :error r))
          (recur (pop syms)))))))

(defn- request-eod-share-prices
  [access-key result-out-chnl symbols]
    (go
      (let [s       (->> symbols (map :symbol) (str/join ","))
            request {:params          {:access_key access-key
                                       :symbols    s}
                     :format          :json
                     :response-format :json
                     :handler         (partial handler result-out-chnl symbols)
                     :error-handler   (partial err-handler result-out-chnl symbols)
                     :keywords?       true}]
        @(GET (str base-url eod-url)
              request))))

(defn- quote-cached-eod-share-prices
  [symbols result-out-chnl]
  (filterv (fn [{s :symbol :as s-info}]
             (let [share-price (caching/get-cache-item *cache (cache-key s))]
               (if (= ::caching/nil-cache-item share-price)
                s-info
                #_else (let [_ (>!! result-out-chnl share-price)]))))
          symbols))

(defn quote-eod-share-prices
  [access-key symbols & {:keys [batch-size]}]
  (if (seq symbols)
    (let [buffer-size     (count symbols)
          result-out-chnl (chan buffer-size)
          symbols         (quote-cached-eod-share-prices symbols result-out-chnl)
          batch-size      (or batch-size 2)
          batches         (if (< (count symbols) batch-size)
                            [symbols]
                            (vec (partition-all batch-size symbols)))]
      (go
        (doseq [b batches]
          (request-eod-share-prices access-key result-out-chnl b)))
      (as/take buffer-size result-out-chnl))
    ;; No `symbols` were specified, so just return a closed channel,
    ;; that will return `nil` when read from.
    (let [c (as/chan)]
      (as/close! c)
      c)))

(defn quote-eod-share-prices-v2
  [access-key & {:keys [symbol-ticker date-from date-to]}]
  (let [res (promise)]

    res))

(comment
  (let [result-chnl (quote-eod-share-prices
                     dev-access-key
                     [{:symbol "AAPL"}
                      {:symbol "INTC"}])]
    (println (<!! result-chnl))
    (println (<!! result-chnl)))

  (utils/get-cache-item *cache (cache-key "AAPL"))

  (let [request {:params          {:access_key access-key
                                   :symbols    "AAPL"}
                 :format          :json
                 :response-format :json
                 :handler         handler
                 :error-handler   err-handler
                 :keywords?       true}]
    @(GET (str base-url eod-url)
         request))

  )
