(ns reason-alpha.model.core
  (:require [clojure.core.cache :as cache]
            [clojure.pprint]
            [malli.core :as m]
            [reason-alpha.utils :as utils]
            [traversy.lens :as tl]))

(def *model (atom {}))

(def *model-keys-mapping (atom {}))

(defn get-type-key [ns-key schema]
  (->> schema
       meta
       seq
       (some (fn [[k v]] (when (true? v) k)))))

(defn +def! [add-m]
  (let [updtd-m          (merge @*model add-m)
        updtd-m-keys-map (->> add-m
                              (map (fn [[k v]]
                                     (when-let [tpe-k (get-type-key k v)]
                                       [tpe-k k])))
                              (remove nil?)
                              (into {})
                              (merge @*model-keys-mapping))]
    (reset! *model updtd-m)
    (reset! *model-keys-mapping updtd-m-keys-map)
    add-m))

(defmacro def-model [sym new-m]
  `(let [ns#      (str *ns*)
         sym-nm#  (name '~sym)
         malli-k# (keyword (str ns# "/" sym-nm#))
         add-def# {malli-k# ~new-m}]
     (do
       (def ~sym ~new-m)
       (+def! add-def#))))

(comment
  (str *ns*)

  (macroexpand-1 '(def-model Test [:the :model]))

  (def-model Test ^:test [:the :model])

  (reset! *model-keys-mapping {})

  )

(defn get-def [model-k]
  (let [ns-model-k (get @*model-keys-mapping model-k)]
    [:schema {:registry @*model} ns-model-k]))

(defn validate [model-k entity]
  (m/validate (get-def model-k) entity))

(defn entity-type [entity-map]
  (-> entity-map
      (tl/view-single
       (tl/*>
        tl/all-keys
        (tl/conditionally #(= (name %) "creation-id"))))
      namespace))

(defn entity-id-key [entity-map]
  (let [ent-type (entity-type entity-map)]
    (keyword (str ent-type "/id"))))

(defn handler-fns [aggregates]
  (-> aggregates
      (tl/update
       tl/all-entries
       (fn [[aggr-k {cmds  :commands
                     qries :queries}]]
         (letfn [(to-ns-keys [{:keys [commands queries]}]
                   (-> commands
                       (or queries)
                       (tl/update
                        tl/all-keys
                        #(-> aggr-k
                             name
                             (str "." (if commands "command" "query") "/" (name %))
                             keyword))))]
           (merge (to-ns-keys {:commands cmds})
                  (to-ns-keys {:queries qries})))))))

(comment

  

  (validate :trade-pattern {:trade-pattern/id          #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"
                            :trade-pattern/creation-id #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"
                            :trade-pattern/name        "-"})


  ((m/validator [:map
                 [:trade-pattern/creation-id :uuid]
                 [:trade-pattern/id {:optional true} :uuid]
                 [:trade-pattern/name {:optional true} [:string {:min 1}]]])
   {:trade-pattern/id          #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"
    :trade-pattern/creation-id #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"
    :trade-pattern/name        "-"})

  (let [validate
        (m/validate [:schema  {:registry
                               {"trade-pattern"    [:map
                                                    [:trade-pattern/creation-id :uuid]
                                                    [:trade-pattern/id {:optional true} :uuid]
                                                    [:trade-pattern/name [:string {:min 1}]]
                                                    [:trade-pattern/description {:optional true} [:string {:min 1}]]
                                                    [:trade-pattern/ancestors-path {:optional true} [:sequential :string]]]
                                "service-provider" [:map
                                                    [:service-provider/creation-id :uuid]
                                                    [:service-provider/id {:optional true} :uuid]
                                                    [:service-provider/name [:string {:min 1}]]
                                                    [:service-provider/services {:optional true}
                                                     [:sequential [:enum :broker :share-price-data]]]]
                                "fin-security"     [:map
                                                    [:fin-security/creation-id :uuid]
                                                    [:fin-security/id {:optional true} :uuid]
                                                    [:fin-security/name [:string {:min 1}]]
                                                    [:fin-security/symbols {:optional true}
                                                     [:sequential [:map
                                                                   [:fin-security/symbol [:string {:min 1}]
                                                                    :service-provider/id :uuid]]]]]}}
                     "trade-pattern"]
                    {:trade-pattern/id          #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"
                     :trade-pattern/creation-id #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"
                     :trade-pattern/name        "-"})]
    validate)

  (m/validate [:map
               [:trade-pattern/creation-id :uuid]
               [:trade-pattern/id {:optional true} :uuid]
               [:trade-pattern/name [:string {:min 1}]]
               [:trade-pattern/description {:optional true} [:string {:min 1}]]
               [:trade-pattern/ancestors-path {:optional true} [:sequential :string]]]
              {:trade-pattern/id          #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"
               :trade-pattern/creation-id #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"
               :trade-pattern/name        000})



  )


