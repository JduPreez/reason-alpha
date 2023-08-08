(ns reason-alpha.integration.marketstack-api-client
  (:require [ajax.core :as ajax :refer [GET]]
            [clojure.core.async
             :as async
             :refer [>! <! >!! <!! go chan buffer close!
                     alts! go-loop]]
            [clojure.string :as str]
            [outpace.config :refer [defconfig]]
            [reason-alpha.utils :as utils]
            [tick.core :as tick]))

(def base-url "http://api.marketstack.com/v1/")

(def eod-url "eod/latest")

(def *cache (utils/ttl-cache))

(defn cache-key
  [symbol]
  (str (tick/today) "-" symbol))

(defn handler [result-out-chnl _ {:keys [data] :as r}]
  (clojure.pprint/pprint r)
  (go-loop [d data]
    (when-let [{:keys [symbol]
                :as   share-price} (peek d)]
      (do
        (>! result-out-chnl share-price)
        (utils/set-cache-item *cache (cache-key symbol) share-price)
        (recur (pop d))))))

(defn err-handler [result-out-chnl symbols {:keys [status status-text] :as r}]
  (clojure.pprint/pprint {:error (str "something bad happened: " status " " status-text)})
  (go-loop [syms symbols]
    (when-let [s (peek symbols)]
      (do
        (>! result-out-chnl {:symbol s
                             :error  r})
        (recur (pop syms))))))

(defn- request-eod-share-prices
  [access-key result-out-chnl symbols]
    (go
      (let [s       (str/join "," symbols)
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
  (filterv (fn [s]
             (let [share-price (utils/get-cache-item *cache (cache-key s))]
               (if (= ::utils/nil-cache-item share-price)
                s
                (let [_ (>!! result-out-chnl share-price)]))))
          symbols))

(defn quote-eod-share-prices
  [access-key symbols & {:keys [batch-size]}]
  (let [buffer-size     (count symbols)
        result-out-chnl (chan buffer-size)
        symbols         (quote-cached-eod-share-prices symbols result-out-chnl)]
    (if (seq symbols)
      (let [batch-size (or batch-size 2)
            batches    (if (< (count symbols) batch-size)
                         [symbols]
                         (vec (partition-all batch-size symbols)))]
        (go
          (doseq [b batches]
            (request-eod-share-prices access-key result-out-chnl b)))))
    (async/take buffer-size result-out-chnl)))

(comment
  (let [result-chnl (quote-eod-share-prices
                     access-key
                     ["AAPL" "INTC"])]
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
