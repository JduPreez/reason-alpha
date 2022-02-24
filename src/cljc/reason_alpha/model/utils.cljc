(ns reason-alpha.model.utils)

(defn creation-id-key-by-type [type]
  (-> type
      name
      (str "/creation-id")
      keyword))

(defn entity-ns [m]
  (-> (keys m)
      first
      namespace))

(defn creation-id-key [m]
  (keyword (str (entity-ns m) "/creation-id")))

(defn id-key-by-type [type]
  (-> type
      name
      (str "/id")
      keyword))

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

(defn get-model-members-of [schema member-k]
  (let [member        (->> schema
                           rest
                           (into {})
                           member-k
                           rest)
        maybe-props   (first member)
        has-props?    (map? maybe-props)
        child-members (if has-props?
                        (rest member)
                        member)]

    {:properties maybe-props
     :members    child-members}))

(comment
  (letfn [(get-members-of [schema member-k]
            (let [member        (->> schema
                                     rest
                                     (into {})
                                     member-k
                                     rest)
                  maybe-props   (first member)
                  has-props?    (map? maybe-props)
                  child-members (if has-props?
                                  (rest member)
                                  member)]

              {:properties maybe-props
               :members    child-members})
            )]
    (let [schema [:map
                  [:symbol/ticker [:string {:min 1}]]
                  [:symbol/instrument-id uuid?]
                  [:symbol/provider
                   [:enum {:enum/titles {:yahoo-finance "Yahoo! Finance"
                                         :saxo-dma      "Saxo/DMA"
                                         :easy-equities "Easy Equities"}}
                    :yahoo-finance :saxo-dma :easy-equities]]]]
      (get-members-of schema :symbol/provider)))


  )
