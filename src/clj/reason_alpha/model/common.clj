(ns reason-alpha.model.common
  (:require [axel-f.excel :as axel-f]
            [clojure.set :as s]
            [clojure.zip :as z]
            [malli.core :as m]
            [malli.util :as mu]
            [pact.core :refer [then error]]
            [reason-alpha.data-structures :as data-structs]
            [reason-alpha.model.accounts :as accounts]
            [reason-alpha.model.core :as model :refer [def-model]]
            [reason-alpha.model.fin-instruments :as fin-instruments]
            [taoensso.timbre :as timbre :refer (warnf errorf debug)]))

(def ^:dynamic *context* {})

(defn result-schema [result-schema]
  [:map
   [:result-id {:description (str "A result that must be "
                                  "displayed on the UI must "
                                  "have a unique id")
                :optional?   true} uuid?]
   [:result {:optional true} result-schema]
   [:type [:enum :error :success :warn :info :failed-validation]]
   [:title {:optional true} string?]
   [:error {:optional true} any?]
   [:description {:optional true}
    [:or string? [:sequential string?]]]
   [:nr-items {:optional true} int?]])

(def-model getContext
  :model/get-context
  [:=> :cat
       [:map
        [:send-message :any]
        [:*connected-users :any]
        [:user-account (mu/union
                        accounts/Account
                        [:map
                         [:account/currency {:optional true}
                          fin-instruments/Currency]])]]])

(m/=> get-context getContext)

(defn get-context []
  *context*)

;; TODO: Memoize this
(defn schema
  ([model]
   (schema model nil))
  ([model registry]
   (m/schema [:schema (when registry
                        {:registry registry})
              model])))

(def schema-m (memoize schema))

(defn computations [model]
  (let [s (schema-m model)]
    (m/walk
     s
     (fn walk-schema
       [schema' path children {::keys [*definitions] :as opts}]
       (let [s-type (m/type schema')]
         (if (and (= s-type :map)
                  (seq children))
           (->> children
                (reduce
                 (fn [reduced' [k opts s]]
                   (let [c (get opts :compute)]
                     (if c
                       (assoc! reduced' k c)
                       reduced')))
                 (transient {}))
                (persistent!))
           (first children)))))))

;; ;; metadata of compiled functions has information about used variables
;; (meta (axel-f/compile "SUM(1, 2, AVERAGE({4,5,6}), foo.bar, foo.baz[*].x)"))
;; ;; => {:free-variables (("foo" "bar") ("foo" "baz" "*" "x")) ... }
;; ^: Columns will not have a path. So we can match `:free-variables` with column names & build
;; a dependency graph that way, to determine the sequence of calculations to apply.

(defn compute-order [computations]
  (let [get-root-comps         (comp #(s/difference (set (keys computations)) %)
                                     set
                                     #(mapcat (fn [[k {reqr :require}]]
                                                reqr) %))
        root-comps             (get-root-comps computations)
        root-k                 ::*root*
        computations           (assoc computations root-k {:require (vec root-comps)})
        zcomps                 (z/zipper #(some? (-> % computations :require))
                                         #(-> % computations :require)
                                         identity
                                         root-k)
        comps-seq              (fn comps-seq [n]
                                 (lazy-seq
                                  (when-not (z/end? n)
                                    (cons n (comps-seq (z/next n))))))
        {:keys [comp-paths
                longest-path]} (->> (comps-seq zcomps)
                                    (reduce (fn [{:keys [longest-path comp-paths] :as reslt} n]
                                              (if (z/branch? n)
                                                reslt
                                                (let [p            (->> n
                                                                        z/node
                                                                        (conj (z/path n))
                                                                        reverse)
                                                      plength      (count p)
                                                      longest-path (if (> plength longest-path)
                                                                     plength
                                                                     longest-path)]
                                                  (merge reslt
                                                         {:comp-paths   (conj comp-paths p)
                                                          :longest-path longest-path}))))
                                            {:comp-paths   []
                                             :longest-path 0}))
        get-comp-order         (comp #(remove #{root-k} %)
                                     distinct
                                     #(remove nil? %)
                                     #(apply interleave %)
                                     #(map (fn [p]
                                             (let [c (count p)]
                                               (if (< c longest-path)
                                                 (concat (repeat (- longest-path c) nil) p)
                                                 p))) %))
        comp-order             (get-comp-order comp-paths)]
    comp-order))

(def compile-str (memoize
                  #(let [fn-str (format "WITH(PERCENT, FN(n, ROUND(n * 100, 2)),
                                              TPERCENT, FN(n, PERCENT(n) & '%%'), %s)" %)]
                             (axel-f/compile fn-str))))

(defn compute
  [data {:keys [computations group-ref-key id-key sub-items-key]
         :as   opts}]
  (try
    (let [data       (if group-ref-key
                       (data-structs/hierarchy->nested-maps data {:group-ref-key group-ref-key
                                                                  :id-key        id-key})
                       data)
          comp-order (compute-order computations)
          data       (->> data
                          (mapv
                           #(reduce (fn [d comp-k]
                                      (let [{comp-str :function
                                             use'     :use
                                             req      :require} (computations comp-k)
                                            fn-comp             (compile-str comp-str)
                                            any-nils?           (->> req
                                                                     (concat use')
                                                                     (map (fn [u] (get d u)))
                                                                     (not-every? some?))
                                            comp-v              (when-not any-nils?
                                                                  (fn-comp d))]
                                        (assoc d comp-k comp-v)))
                                    %
                                    comp-order)))]
      {:result data
       :type   :success})
    (catch Exception e
      (let [err-msg "Failed to compute dynamic functions"]
        {:error       (ex-data e)
         :description (str err-msg ":" (ex-message e))
         :type        :error}))))
