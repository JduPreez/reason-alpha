(ns reason-alpha.view-model)

(defn model->view-component [[member-nm & schema] & [col-def]]
  (let [{can-sort :can-sort
         menu     :menu
         :or      {can-sort true}} col-def
        props-or-type              (first schema)
        has-props?                 (map? props-or-type)
        {:keys [title ref]}        props-or-type
        ref-nm                     (when ref
                                     (name ref))
        ref-ns                     (when ref
                                     (namespace ref))
        type                       (if has-props?
                                     (second schema)
                                     props-or-type)]
    (cond
      (and ref-ns
           ref) {:title             title
                 :name              member-nm
                 :type              :select
                 :data-subscription [(keyword ref-ns (str ref-nm "-options"))]
                 :can-sort          can-sort}
      ref       {:title             title
                 :name              member-nm
                 :type              :select
                 :data-subscription [(keyword ref-nm "options")]
                 :can-sort          can-sort}
      :default  {:title    title
                 :name     member-nm
                 :can-sort can-sort})))

(namespace :nbm/jjk)
