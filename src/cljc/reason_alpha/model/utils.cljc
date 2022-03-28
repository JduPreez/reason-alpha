(ns reason-alpha.model.utils
  (:require [clojure.string :as str]))

(defn creation-id-key-by-type [type]
  (-> type
      name
      (str "-creation-id")
      keyword))

(defn entity-ns [m]
  (-> (keys m)
      first
      namespace))

(defn id-key-by-type [type]
  (-> type
      name
      (str "-id")
      keyword))

(defn some-ns-key [key entities]
  (let [key-nm (name key)]
    (->> entities
         first
         keys
         (some #(when (= key-nm (name %))
                  %)))))

(defn- -id-key [model-type key-nm m]
  (let [mtype-nm (name model-type)
        ent-id-k (keyword mtype-nm key-nm)
        dto-id-k (keyword (str mtype-nm "-" key-nm))]
    (if (contains? m ent-id-k)
      ent-id-k
      dto-id-k)))

(defn creation-id-key [model-type m]
  (-id-key model-type "creation-id" m))

(defn id-key [model-type m]
  (-id-key model-type "id" m))

(defn merge-by-id [model-type maps1 maps2]
  (let [m         (first (or maps2 maps1))
        id-k      (id-key model-type m)
        crtn-id-k (creation-id-key model-type m)
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
                           (some #(when (= member-k (first %)) %)))
        type-spec     (last member)
        maybe-props   (second type-spec)
        has-props?    (map? maybe-props)
        child-members (if has-props?
                        (nnext type-spec)
                        (next type-spec))]
    {:properties maybe-props
     :members    child-members}))
