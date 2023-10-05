(ns reason-alpha.infrastructure.caching
  (:require [outpace.config :refer [defconfig]]
            [clojure.core.cache :as cache]))

(defconfig settings)

(defn ttl-memoize
  [f & {ttl :ttl, fn-cache-key :fn-cache-key, :or {ttl          3600000 ;; TTL 1 hour
                                                   fn-cache-key (fn [args]
                                                                  (println "calling default cache key fn")
                                                                  args)}}]
  (let [mem (atom (cache/ttl-cache-factory {} :ttl ttl))]
    (fn [& args]
      (let [cache-k (fn-cache-key args)
            e       (cache/lookup @mem cache-k ::nil)]
        (if (= ::nil e)
          (let [_   (println "calling function " cache-k)
                ret (apply f args)]
            (swap! mem cache/miss cache-k ret)
            ret)
          (do
            (println "getting from cache")
            (swap! mem cache/hit cache-k)
            e))))))

(defn ttl-cache
  [& {ttl :ttl, :or {ttl 3600000}}]
  (atom (cache/ttl-cache-factory {} :ttl ttl)))

(defn get-cache-item
  [*cache k]
  ;; Use `:cache.item/nil` to enable caching `nil` values
  (let [i (cache/lookup @*cache k ::nil-cache-item)]
    (if (= ::nil-cache-item i)
      ::nil-cache-item
      (do
        (swap! *cache cache/hit k)
        i))))

(defn set-cache-item
  [*cache k v]
  (swap! *cache cache/miss k v))

(defn wrap [f & {:keys [fn-cache-key]}]
  (let [{:keys [cache? ttl-timespan]} settings]
    (if cache?
      (ttl-memoize f
                   :fn-cache-key (or fn-cache-key
                                     #(identity %))
                   :ttl (or ttl-timespan
                            ;; 1 hour
                            3600000))
      #_else f)))

