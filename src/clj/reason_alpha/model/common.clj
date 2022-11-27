(ns reason-alpha.model.common
  (:require [axel-f.excel :as axel-f]
            [clojure.set :as s]
            [clojure.zip :as z]
            [malli.core :as m]
            [pact.core :refer [then error]]
            [reason-alpha.data-structures :as data-structs]
            [reason-alpha.model.accounts :as accounts]
            [reason-alpha.model.core :as model :refer [def-model]]
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
                                             use'     :use} (computations comp-k)
                                            fn-comp         (compile-str comp-str)
                                            any-nils?       (->> use'
                                                                 (map (fn [u] (get d %)))
                                                                 (not-every? some?))
                                            comp-v          (when-not any-nils?
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

(comment
  (let [d         {:holding              [#uuid "01809f38-c167-6811-e9ef-c2edd166236d" "Unity"],
                   :open-price           33,
                   :open-time            #inst "2022-09-23T00:00:00.000-00:00",
                   :position-creation-id #uuid "4daabc0e-7c8b-45c6-a1b5-6a8b53a7fc64",
                   :status               :open,
                   :close-price          84.86,
                   :position-id          #uuid "0183094e-11b0-aa9b-1cc5-2e5fdd6d5c76",
                   :holding-position-id  #uuid "0182e013-fc70-acc3-3366-5e9617cceb8a",
                   :holding-id           #uuid "01809f38-c167-6811-e9ef-c2edd166236d",
                   :quantity             33,
                   :eod-historical-data  "U.US",
                   :long-short           [:long ""]}
        use'      [:stop-loss :quantity :open-price] 
        any-nils? (-> d
                      (select-keys use')
                      vals
                      (as-> v (not-every? some? v)))
        fn-comp   (compile-str "TPERCENT(stop-loss/(quantity * open-price))")]
    (when-not any-nils? (fn-comp d)))
  

  (let [fn-comp (compile-str "TPERCENT(stop-loss/(quantity * open-price))")
        d       {:holding              [#uuid "01809f38-c167-6811-e9ef-c2edd166236d" "Unity"], 
                 :open-price           33, 
                 :open-time            #inst "2022-09-23T00:00:00.000-00:00", 
                 :position-creation-id #uuid "4daabc0e-7c8b-45c6-a1b5-6a8b53a7fc64", 
                 :status               :open, 
                 :close-price          27.39, 
                 :position-id          #uuid "0183094e-11b0-aa9b-1cc5-2e5fdd6d5c76", 
                 :holding-position-id  #uuid "0182e013-fc70-acc3-3366-5e9617cceb8a", 
                 :holding-id           #uuid "01809f38-c167-6811-e9ef-c2edd166236d", 
                 :quantity             33, 
                 :eod-historical-data  "U.US",
                 :stop-loss            898.78
                 :long-short           [:long ""]}]
    (fn-comp d))

  (let [data  [{:stop-loss  -760
                :quantity   152
                :open-price 71.83}
               {:stop-loss  -7878
                :quantity   344
                :open-price 562}
               {:stop-loss  -901
                :quantity   23
                :open-price 184.8}
               {:stop-loss  -215
                :quantity   512
                :open-price 87.11}]
        comps {:stop-percent-loss
               {:function
                "PERCENT(stop-loss/(quantity * open-price))"}
               :xyz
               {:require [:stop-percent-loss]
                :function
                "100 - IF(stop-percent-loss < 0, stop-percent-loss * -1, stop-percent-loss)"}
               :multiply-by-2
               {:require [:stop-percent-loss :xyz]
                :function
                "ROUND((xyz + stop-percent-loss) * 2, 2)"}}]
    (compute data {:computations comps})
    )

  (let [computations           {:column1 {:require  [:column2 :column3]
                                          :use      [:column1 :column4]
                                          :function ""}
                                :column2 {:function ""}
                                :column3 {:require [:column4 :column5]}
                                :column4 {:require [:column5]}
                                :column5 {}}
        get-root-comps         (comp #(s/difference (set (keys computations)) %)
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
    comp-order
    )

   )
