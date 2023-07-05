(ns reason-alpha.views.utils)

(def ^:const ref-suffix "ref")

(def ref-suffix-list (str ref-suffix "-list"))

(defn ref->data-sub
  [ref]
  (when ref
    (let [ref-nm (name ref)
          ref-ns (namespace ref)]
      (if ref-ns
        [(keyword ref-ns (str ref-nm "-" ref-suffix-list))]
        [(keyword ref-nm ref-suffix-list)]))))

(defn ref->parent-data-sub
  [ref]
  (let [ref-nm (when ref (name ref))
        ref-ns (when ref (namespace ref))]
    (if ref-ns
      (keyword ref-ns (str ref-nm "-" ref-suffix))
      (keyword ref-nm ref-suffix))))
