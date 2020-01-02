(ns reason-alpha.utils
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defn list-resource-files [dir file-type]
  (let [xtnsn (str "." file-type)]
    (-> dir
        io/resource
        io/file
        file-seq
        ((fn [coll]
           (filter #(string/ends-with? % xtnsn) coll))))))

(defn edn-file->clj [file]
  (-> file
      slurp
      edn/read-string))

(defn edn-files->clj [dir]
  (->> (list-resource-files dir "edn")
       (map edn-file->clj)))