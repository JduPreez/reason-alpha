(ns reason-alpha.model.common
  (:require [malli.core :as m]
            [reason-alpha.model.core :as model :refer [def-model]]
            [reason-alpha.model.accounts :as accounts]
            [pact.core :refer [then error]]))

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

(defn process-data-funcs
  [data {:keys [functions group-ref-fnk id-fnk sub-items-key]
         :as   opts}]
  (let [;;idx-data (map (fn [i] [(id-fnk i) i]) data)
        data (if group-ref-fnk
               (->> data
                    (map [item]
                         (let [id (id-fnk item)
                               sub-items ]
                           (->> data
                                (filter #(= (group-ref-fnk %) id))
                                (assoc item sub-items-key)))))
               data)]
    data))

(defn deepen [steps]
  (->> steps
       (reduce (fn [loc step]
                 (case step
                   -1 (-> loc
                          (zip/append-child [])
                          (zip/down)
                          (zip/rightmost))
                   0  (zip/up loc)
                   (zip/append-child loc step)))
               (zip/vector-zip []))
       (zip/root)))

(comment
  (require '[clojure.zip :as z])

  (let [group-ref-key :parent-id
        id-key        :id
        create-zipper (fn create-zipper [s]
                        (let [g (group-by group-ref-key s)]
                          (z/zipper g #(map id-key (g %)) nil nil #_(-> nil g first id-key))))
        data          [{:id          989
                        :description "AAAAA"}
                       {:id          6544
                        :description "BBBBB"
                        :parent-id   989}
                       {:id          2144
                        :description "CCCCC"
                        :parent-id   989}
                       {:id          1001
                        :description "DDDD"}
                       {:id          4444
                        :description "EEEEE"
                        :parent-id   1001}]
        zd            (create-zipper data)]
    (-> zd z/next z/next))

  ;; https://stackoverflow.com/questions/18779718/how-to-transform-a-seq-into-a-tree
  ;; https://stackoverflow.com/questions/31549430/clojure-flat-sequence-into-tree
  )
