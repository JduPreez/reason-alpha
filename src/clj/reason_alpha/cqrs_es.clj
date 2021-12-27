(ns reason-alpha.cqrs-es
  (:require [traversy.lens :as tl]))

;; https://cqrs.nu/tutorial/cs/02-domain-logic

;; Thoughts 2021-12-20:
;; 1. Save the entity as usual. This becomes the aggregate read model.
;; 2. Save events separately to 'event entity'. The event is an 'entity' by itself.
;;    * Attach the tx-id of the persisted change from point 1 to the 'event entity'

(def aggregates-conf
  {:trade-pattern
   {:cqrs/commands {:save #:cqrs{:handler-fn nil
                                 :events     [:trade-pattern.event/saved
                                              :trade-pattern.event/save-failed]}}
    :cqrs/queries  {:get  {:cqrs/query-fn nil}
                    :get1 {:cqrs/query-fn nil}}}
   :position
   {:cqrs/commands {:save {:cqrs/handler-fn nil
                           :cqrs/events     [:position.event/saved
                                             :position.event/save-failed]}}
    :cqrs/queries  {:get  {:cqrs/query-fn nil}
                    :get1 {:cqrs/query-fn nil}}}})

(defn get-handler-fns [aggregates]
  (-> aggregates
      (tl/update
       tl/all-entries
       (fn [[aggr-k {cmds  :cqrs/commands
                     quers :cqrs/queries}]]
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
                  (to-ns-keys {:queries quers})))))))

(comment

  (get-handler-fns aggregates-conf)


  (-> aggregates
      (tl/view
       (tl/*> tl/all-entries
              (tl/in [1])
              (tl/select-entries [:cqrs/commands])
              (tl/in [1]))
       )
      )

  

  (keyword "trade/tyiotle")

  (aggregates #{:trade-pattern :position})

  #_[{:trade-pattern ;; Aggregate/service
      {:cqrs/commands {:save nil} ;; Command malli schema
       :cqrs/events   {:trade-pattern/saved       nil ;; Event malli schema
                       :trade-pattern/save-failed nil}
       :cqrs/handlers {:save ;; Command it handles
                       {:cqrs/events     [:trade-pattern/saved :trade-pattern/save-failed] ;; Malli schema names
                        :cqrs/handler-fn #'reason-alpha.domain.trades/do-something}}
       ;; We can have a default implementation for apply, so that it uses this by default if `:cqrs/apply`
       ;; isn't specified
       :cqrs/appliers {:trade-pattern/saved #'reason-alpha.domain.trades/apply-event}}}]

  )
