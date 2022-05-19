(ns reason-alpha.utils
  #?(:clj (:require [clojure.edn :as edn]
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
   (defn log [location {:keys [type description error] :as log}]
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

#?(:clj
   (defn round [number & [dec-places]]
     (let [dec-places (or dec-places 2)]
       (-> number
           BigDecimal/valueOf
           (.setScale dec-places BigDecimal/ROUND_HALF_UP)
           .floatValue))))

#?(:cljs
   (defn maybe-parse-number [nr]
     (cond
       (re-find #"^-?\d+\.\d+$" nr) (js/parseFloat nr)
       (re-find #"^-?\d+$" nr)      (js/parseInt nr)
       :else                        nr)))
