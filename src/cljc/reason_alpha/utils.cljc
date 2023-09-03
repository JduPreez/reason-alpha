(ns reason-alpha.utils
  #?(:clj (:require [clojure.core.cache :as cache]
                    [clojure.edn :as edn]
                    [clojure.java.io :as io]
                    [clojure.string :as str]
                    [taoensso.timbre :as timbre :refer-macros (tracef debugf infof warnf errorf)])

     :cljs (:require [clojure.string :as str]
                     [cljs-uuid-utils.core :as uuid]
                     [goog.string :as gstring]
                     [goog.string.format]
                     [taoensso.timbre :as timbre :refer-macros (infof warnf errorf)]))
  #?(:clj (:import [java.math BigDecimal])))

(defn maybe->uuid [v]
  #?(:clj (try
            (java.util.UUID/fromString v)
            (catch Exception _e v))
     :cljs (if (uuid/valid-uuid? v)
              (uuid/make-uuid-from v)
              v)))

(defn not-blank? [txt]
  (and (string? txt)
       (not (str/blank? txt))))

(defn new-uuid []
  #?(:clj (java.util.UUID/randomUUID)
     :cljs (random-uuid)))

(defn get-ns [a-var]
  (-> a-var meta :ns))

(defn rand-between [min max]
  (+ (* (rand max) (- max min)) min))

(defn keyword->str [k]
  (subs (str k) 1))

(defn str-keys [items]
  (map #(into {} (for [[k v] %]
                   [(keyword->str k) v])) items))

#?(:cljs
   (defn format
     "Supported substitutions s, f, d, i and u"
     [& args]
     (apply gstring/format args)))

(defn kw-keys [item]
  (into {} (for [[k v] item]
             [(keyword k) v])))

#?(:cljs
   (defn log
     [location {:keys [type description error] :as log}]
     (let [l {location log}]
       (case type
         :error
         , (do (errorf "%s %s" location description)
               (cljs.pprint/pprint {:log/error l}))
         :warn
         , (do (warnf "%s %s" location description)
               (cljs.pprint/pprint {:log/warn l}))
         (cljs.pprint/pprint {:log/info l})))))

#?(:clj
   (defn ttl-memoize
     [f & {ttl :ttl, :or {ttl 3600000}}] ;; TTL 1 hour
     (let [mem (atom (cache/ttl-cache-factory {} :ttl ttl))]
       (fn [& args]
         (let [e (cache/lookup @mem args ::nil)]
           (if (= ::nil e)
             (let [ret (apply f args)]
               (swap! mem cache/miss args ret)
               ret)
             (do
               (swap! mem cache/hit args)
               e)))))))

#?(:clj
   (defn ttl-cache
     [& {ttl :ttl, :or {ttl 3600000}}]
     (atom (cache/ttl-cache-factory {} :ttl ttl))))

#?(:clj
   (defn get-cache-item
     [*cache k]
     ;; Use `:cache.item/nil` to enable caching `nil` values
     (let [i (cache/lookup @*cache k ::nil-cache-item)]
       (if (= ::nil-cache-item i)
         ::nil-cache-item
         (do
           (swap! *cache cache/hit k)
           i)))))

#?(:clj
   (defn set-cache-item
     [*cache k v]
     (swap! *cache cache/miss k v)))

#?(:clj
   (defn list-resource-files [dir file-type]
     (let [xtnsn (str "." file-type)]
       (-> dir
           io/resource
           io/file
           file-seq
           ((fn [coll]
              (filter #(str/ends-with? % xtnsn) coll)))))))

#?(:clj
   (defn edn-file->clj [file]
     (-> file
         slurp
         edn/read-string)))

#?(:clj
   (defn edn-files->clj [dir]
     (->> (list-resource-files dir "edn")
          (map edn-file->clj))))

(defn str->bool [s]
  (if (= s "true") true
      false))

(->> 500/23
     #_4.6762882
     (with-precision 10 :rounding BigDecimal/UNNECESSARY)
     .floatValue
     println)

#?(:cljs
   (defn maybe-parse-number
     [nr]
     (cond
       (re-find #"^-?\d+\.\d+$" nr) (js/parseFloat nr)
       (re-find #"^-?\d+$" nr)      (js/parseInt nr)
       :else                        nr)))

(defn ignore
  [r]
  nil)
