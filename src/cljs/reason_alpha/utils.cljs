(ns reason-alpha.utils)

(defn rand-between [min max]
  (+ (* (rand max) (- max min)) min))

(defn keyword->str [k]
  (subs (str k) 1))

(defn str-keys [items]
  (map #(into {} (for [[k v] %] 
                   [(keyword->str k) v])) items))

(defn kw-keys [item]
  (into {} (for [[k v] item]
             [(keyword k) v])))

(defn entity-ns [m]
           (-> (keys m)
               first
               namespace))

#_(defn creation-id-key [type]
  (keyword (str (name type) "/creation-id")))

(defn creation-id-key [m]
  (keyword (str (entity-ns m) "/creation-id")))

(defn id-key [m]
  (keyword (str (entity-ns m) "/id")))

#_(defn id-key [type]
  (keyword (str (name type) "/id")))

#_(defn merge-by-id [type maps1 maps2]
  (let [id-k      (id-key type)
        crtn-id-k (creation-id-key type)
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
                                    %
                                    )) maps2))
                       maps1)
                      (into maps2))]
    updated))

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

(defn log [& args]
  (apply js/console.log (conj (vec (map str (butlast args)))
                              (pr-str (last args)))))

(comment
  (merge-by-id nil #_[{:trade-pattern/creation-id 123
                 :trade-pattern/name       "First Name"}
                {:trade-pattern/creation-id 356
                 :trade-pattern/name        "blah"}]
               [{:trade-pattern/creation-id 123
                 :trade-pattern/name        "2nd Name"}
                {:trade-pattern/creation-id 789
                 :trade-pattern/name        "uchedy smackedy"}])

  )
