(ns reason-alpha.utils
  #?(:clj (:require [clojure.edn :as edn]
                    [clojure.java.io :as io]
                    [clojure.string :as str])
     :cljs (:require [clojure.string :as str])))

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

(defn entity-ns [m]
           (-> (keys m)
               first
               namespace))

(defn creation-id-key [m]
  (keyword (str (entity-ns m) "/creation-id")))

(defn id-key [m]
  (keyword (str (entity-ns m) "/id")))

(defn merge-by-id [maps1 maps2]
  (let [m         (first (or maps2 maps1))
        id-k      (id-key m)
        crtn-id-k (creation-id-key m)
        updated   (-> (remove
                       (fn [m1]
                         (some #(let [id-val1      (id-k m1)
                                      crtn-id-val1 (crtn-id-k m1)
                                      id-val2      (id-k %)
                                      crtn-id-val2 (crtn-id-k %)]
                                  (when (or (and (contains? m1 id-k)
                                                 (= id-val1 id-val2))
                                            (and (not (contains? m1 id-k))
                                                 (= crtn-id-val1 crtn-id-val2)))
                                    %)) maps2))
                       maps1)
                      (into maps2))]
    updated))

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
