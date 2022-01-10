(ns reason-alpha.model.core
  (:require [clojure.core.cache :as cache]
            [malli.core :as m]
            [reason-alpha.utils :as utils]
            [traversy.lens :as tl]))

(def ^:private model-cache
  (atom
   (cache/ttl-cache-factory {} :ttl 1800000))) ;;  30 mins

(defn details []
  (when-not (:model @model-cache)
    (let [m (->> "model"
                 utils/edn-files->clj
                 first
                 (assoc {} :registry))]
      (swap! model-cache assoc :model m)))
  (:model @model-cache))

(defn validate [entity-type entity]
  (m/validate
   [:schema (details) (name entity-type)]
   entity))

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

  (do
    (swap! model-cache assoc :model (->> "model"
                                         utils/edn-files->clj
                                         first
                                         (assoc {} :registry)))
    (swap! model-cache assoc-in [:a :b] 1)
    (swap! model-cache assoc-in [:c :d] 2)
    (swap! model-cache assoc-in [:e :f] 3)
    (Thread/sleep 300)
    (swap! model-cache assoc-in [:a :new-key] :should-last-500)
    (Thread/sleep 300)
    ;; the [:a :b] key should be expired, the [:a :new-key] should be present
    ;; but the cache key is actualy just `:a`
    (:model model-cache)
    
    #_(prn [(get-in @model-cache [:c :d]) (get-in @model-cache [:a :b]) (get-in @model-cache [:a :new-key])]))

  (do
    (details)
    (prn (:model @model-cache))
    (Thread/sleep 500)
    (prn (:model @model-cache)))

  
  (cache/has? model-cache :model) 

  (cache/miss model-cache :model (->> "model"
                                      utils/edn-files->clj
                                      first
                                      (assoc {} :registry)))

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





  )


