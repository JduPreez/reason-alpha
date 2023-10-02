(ns reason-alpha.views.utils
  (:require [clojure.string :as str]
            [reitit.core :as r]))

(def ^:const ref-suffix "ref")

(def ref-suffix-list (str ref-suffix "-list"))

(defn ref->data-sub
  [ref]
  (when ref
    (let [ref-nm (name ref)
          ref-ns (namespace ref)]
      (if ref-ns
        [(keyword ref-ns (str ref-nm "-" ref-suffix-list))]
        [(keyword ref-nm ref-suffix-list)]))))

(defn ref->parent-data-sub
  [ref]
  (let [ref-nm (when ref (name ref))
        ref-ns (when ref (namespace ref))]
    (if ref-ns
      (keyword ref-ns (str ref-nm "-" ref-suffix))
      (keyword ref-nm ref-suffix))))

;; (defn href
;;   "Return relative url for given route. Url can be used in HTML links."
;;   ([k]
;;    (href k nil nil))
;;   ([k params]
;;    (href k params nil))
;;   ([k params query]
;;    (rfe-easy/href k params query)))

;; (defn href
;;   [router view-name]
;;   (let [path  (-> router
;;                  (r/match-by-name view-name)
;;                  r/match->path)
;;         form? (str/starts-with? "/forms")]
;;     ))
