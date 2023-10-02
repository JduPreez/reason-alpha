(ns reason-alpha.model.utils
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.util :as mu]))

(defn creation-id-key-by-type
  [type]
  (-> type
      name
      (str "-creation-id")
      keyword))

(defn entity-ns
  [m]
  (-> (keys m)
      first
      namespace))

(defn id-key-by-type
  [type]
  (-> type
      name
      (str "-id")
      keyword))

(defn some-ns-key
  [key entities]
  (let [key-nm (name key)]
    (->> entities
         first
         keys
         (some #(when (= key-nm (name %))
                  %)))))

(defn- -id-key
  [model-type key-nm m]
  (let [mtype-nm (name model-type)
        ent-id-k (keyword mtype-nm key-nm)
        dto-id-k (keyword (str mtype-nm "-" key-nm))]
    (if (contains? m ent-id-k)
      ent-id-k
      dto-id-k)))

(defn creation-id-key
  [model-type m]
  (-id-key model-type "creation-id" m))

(defn id-key
  [model-type m]
  (-id-key model-type "id" m))

(defn id-member?
  [member-nm]
  (-> member-nm
      name
      (str/ends-with? "-id")))

(defn merge-by-id
  [model-type maps1 maps2]
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

(defn model-member-schema-info
  ([schema member-nm]
   (member-nm (model-member-schema-info schema)))
  ([schema]
   (when schema
     (let [members        (m/entries schema)
           member-sch-inf (->> schema
                               m/children
                               (map-indexed
                                (fn [idx [mbr-nm props sch]]
                                  (let [sch           (if (m/schema? sch)
                                                        (m/form sch)
                                                        #_else sch)
                                        props         (-> sch m/properties
                                                          (merge props))
                                        child-members (->> sch
                                                           m/children
                                                           (mapv (fn [s]
                                                                   (if (m/schema? s)
                                                                     (m/form s)
                                                                     s))))
                                        t             (m/type sch)
                                        t             (if (= :maybe t)
                                                        (peek child-members)
                                                        t)
                                        [val-type _]  (when (and (= :tuple t)
                                                                 (= 2 (count child-members)))
                                                        child-members)]
                                    [mbr-nm {:order           idx
                                             :properties      props
                                             :schema          sch
                                             :type            t
                                             :member-val-type val-type
                                             :members         child-members}])))
                               (into {}))]
       member-sch-inf))))

(defn enum-titles
  [enum-schema]
  (when enum-schema
    (-> enum-schema
        m/properties
        :enum/titles)))
