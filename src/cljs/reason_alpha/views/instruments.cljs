(ns reason-alpha.views.instruments)

(defn fields [providers-model]
  (let [providers-model           (rest providers-model)
        {:keys [provider/titles]} (first providers-model)
        symbol-fields             (for [p    (rest providers-model)
                                        :let [t (get titles pm)]]
                                    {:title    t
                                     :name     p
                                     :can-sort true})]
    (into
     [{:title    "Instrument"
       :name     :instrument-name
       :can-sort true}]
      cat
      symbol-fields
      [{:title "Type"
        }])))

