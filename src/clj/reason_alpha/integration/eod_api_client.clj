(ns reason-alpha.integration.eod-api-client
  (:require [ajax.core :refer [GET]]
            [cheshire.core :as json]
            [clojure.string :as str]
            [outpace.config :refer [defconfig]]))

(defconfig live-stock-prices-api)

(defn- handle-get-live-price [*result response]
  (deliver *result
           {:result response
            :type   :success}))

(defn- handle-get-live-price-err [*result {:keys [status status-text]
                                           :as   response}]
  (deliver *result
           {:error       response
            :description (str "something bad happened: " status " " status-text)
            :type        :error}))

(defn get-live-price [api-token [main-sym & additional-syms]]
  (let [*result             (promise)
        uri                 (format live-stock-prices-api main-sym)
        concat-additnl-syms (str/join "," additional-syms)]
    (GET uri
         {:params          {:api_token api-token
                            :fmt       "json"
                            :s         concat-additnl-syms}
          :handler         #(handle-get-live-price *result %)
          :error-handler   #(handle-get-live-price-err *result %)
          :format          url-request-format
          :response-format :json
          :keywords?       true})
    *result))

