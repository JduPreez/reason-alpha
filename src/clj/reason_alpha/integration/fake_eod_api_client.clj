(ns reason-alpha.integration.fake-eod-api-client)

(defn quote-live-prices [api-token [main-ticker & additional-tickers]]
  (let [*result (promise)]
    *result))
