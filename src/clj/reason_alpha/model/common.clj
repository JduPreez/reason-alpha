(ns reason-alpha.model.common
  (:require [clojure.zip :as z]
            [malli.core :as m]
            [pact.core :refer [then error]]
            [reason-alpha.data-structures :as data-structs]
            [reason-alpha.model.accounts :as accounts]
            [reason-alpha.model.core :as model :refer [def-model]]))

(def ^:dynamic *context* {})

(defn result-schema [result-schema]
  [:map
   [:result {:optional true} result-schema]
   [:type [:enum :error :success :warn :info :failed-validation]]
   [:error {:optional true} any?]
   [:description {:optional true} string?]
   [:nr-items {:optional true} int?]])

(def-model getContext
  :model/get-context
  [:=> :cat
       [:map
        [:send-message :any]
        [:*connected-users :any]
        [:user-account accounts/Account]]])

(m/=> get-context getContext)

(defn get-context []
  *context*)

;; ;; metadata of compiled functions has information about used variables
;; (meta (axel-f/compile "SUM(1, 2, AVERAGE({4,5,6}), foo.bar, foo.baz[*].x)"))
;; ;; => {:free-variables (("foo" "bar") ("foo" "baz" "*" "x")) ... }
;; ^: Columns will not have a path. So we can match `:free-variables` with column names & build
;; a dependency graph that way to determine the sequence of calculations to apply

(defn compute-order [current-comp computations]
  (let [zcomps      (z/zipper #(some? (-> % computations :require))
                              #(-> % computations :require)
                              identity
                              current-comp)
        comp-orders (loop [n          zcomps
                           leaf-paths []]
                      (if (z/end? n)
                        leaf-paths
                        (recur (z/next n)
                               (if (z/branch? n)
                                 (reverse leaf-paths)
                                 (->> n
                                      z/node
                                      (conj (z/path n))
                                      (conj leaf-paths))
                                 ))))]
    comp-orders))

(defn compute
  [data {:keys [computations group-ref-key id-key sub-items-key]
         :as   opts}]
  (let [data (if group-ref-key
               (data-struct/hierarchy->nested-maps data {:group-ref-key group-ref-key
                                                         :id-key        id-key})
               data)]
    (->> data
         (reduce (fn [data [comp-k {:keys [deps function]}]]
                   (let []))))))


(comment
  (let [computations {:column1 {:require  [:column2 :column3]
                                :function ""}
                      :column2 {:function ""}
                      :column3 {:require [:column4 :column5]}
                      :column4 {:require [:column5]}
                      :column5 {}}
        root         :column1
        zcomps       (z/zipper #(some? (-> % computations :require))
                               #(-> % computations :require)
                               identity
                               root)
        comp-paths   (loop [n          zcomps
                            leaf-paths []]
                       (if (z/end? n)
                         leaf-paths
                         (recur (z/next n)
                                (if (z/branch? n)
                                  leaf-paths
                                  (->> n
                                       z/node
                                       (conj (z/path n))
                                       reverse
                                       (conj leaf-paths))))))
        max-path     (->> comp-paths
                          (map #(count %))
                          (apply max))
        comp-paths2  (->> comp-paths
                          (map #(let [c (count %)]
                                  (if (< c max-path)
                                    (concat (repeat (- max-path c) nil) %)
                                    %))))
        comp-path    (->> (apply interleave comp-paths2)
                          (remove nil?)
                          distinct)]
    {:CPS  comp-paths
     :CPS2 comp-paths2
     :CP   comp-path}
    )


   )
