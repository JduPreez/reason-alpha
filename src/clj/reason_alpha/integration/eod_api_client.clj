(ns reason-alpha.integration.eod-api-client
  (:require [ajax.core :refer [GET]]
            [cheshire.core :as json]
            [clojure.string :as str]
            [outpace.config :refer [defconfig]]))

(defconfig live-stock-prices-api)

(defn- handle-quote-live-price [*result response]
  (deliver *result
           {:result response
            :type   :success}))

(defn- handle-quote-live-price-err [*result {:keys [status status-text]
                                           :as   response}]
  (deliver *result
           {:error       response
            :description (str "something bad happened: " status " " status-text)
            :type        :error}))

(defn quote-live-prices [api-token [main-sym & additional-syms]]
  (let [*result             (promise)
        uri                 (format live-stock-prices-api main-sym)
        concat-additnl-syms (str/join "," additional-syms)]
    (GET uri
         {:params          {:api_token api-token
                            :fmt       "json"
                            :s         concat-additnl-syms}
          :handler         #(handle-quote-live-price *result %)
          :error-handler   #(handle-quote-live-price-err *result %)
          :response-format :json
          :keywords?       true})
    *result))

