(ns reason-alpha.utils
  #?(:clj (:require [clojure.edn :as edn]
                    [clojure.java.io :as io]
                    [clojure.string :as str])
     :cljs (:require [clojure.string :as str]
                     [cljs-uuid-utils.core :as uuid])))

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

(defn kw-keys [item]
  (into {} (for [[k v] item]
             [(keyword k) v])))

#?(:cljs
   (defn log [& args]
     (apply js/console.log (conj (vec (map str (butlast args)))
                                 (pr-str (last args))))))

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
