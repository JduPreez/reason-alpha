(ns reason-alpha.web.service-api-test
  (:require [cljs.test :as t :refer-macros [deftest is testing async run-tests]]
            [reason-alpha.web.service-api :as svc-api]))

(defn entity [& [with-id]]
  {:trade-pattern/id          (when with-id #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6")
   :trade-pattern/name        "Pullback"
   :trade-pattern/description ""
   :trade-pattern/parent-id   nil
   :trade-pattern/user-id     #uuid "8ffd2541-0bbf-4a4b-adee-f3a2bd56d83f"})

(deftest test-entity-action->http-request
  (testing "`entity-action->http-request` should create a HTTP request for updating an entity"
    (let [{:keys [trade-pattern/id]
           :as   ent}              (entity :with-id)
          uri-regex-pattern        (re-pattern
                                    (str "^http://\\w*:?\\d*?/api/trade-patterns/" id "$"))
          {{:keys [method
                   uri]} :http-xhrio
           :as           http-req} (svc-api/entity-action->http-request
                                    {:entities-type :trade-patterns
                                     :action        :save
                                     :data          ent})
          [uri-regex-match]        (re-seq uri-regex-pattern uri)]
      (is http-req)
      (is (= method :put))
      (is (= uri uri-regex-match))))

  (testing "`entity-action->http-request` should create a HTTP request for creating an entity"
    (let [ent                      (entity)
          uri-regex-pattern        (re-pattern
                                    "^http://\\w*:?\\d*?/api/trade-patterns$")
          {{:keys [method
                   uri]} :http-xhrio
           :as           http-req} (svc-api/entity-action->http-request
                                    {:entities-type :trade-patterns
                                     :action        :save
                                     :data          ent})
          [uri-regex-match]        (re-seq uri-regex-pattern uri)]
      (is http-req)
      (is (= method :post))
      (is (= uri uri-regex-match))))

  (testing "`entity-action->http-request` should create a HTTP request for deleting an entity"
    (let [{:keys [trade-pattern/id]
           :as   ent}              (entity :with-id)
          uri-regex-pattern        (re-pattern
                                    (str "^http://\\w*:?\\d*?/api/trade-patterns/" id "$"))
          {{:keys [method
                   uri]} :http-xhrio
           :as           http-req} (svc-api/entity-action->http-request
                                    {:entities-type :trade-patterns
                                     :action        :delete
                                     :data          ent})
          [uri-regex-match]        (re-seq uri-regex-pattern uri)]
      (is http-req)
      (is (= method :delete))
      (is (= uri uri-regex-match))))

  (testing "`entity-action->http-request` should create a HTTP request for fetching all entities"
    (let [uri-regex-pattern        (re-pattern
                                    "^http://\\w*:?\\d*?/api/trade-patterns$")
          {{:keys [method
                   uri]} :http-xhrio
           :as           http-req} (svc-api/entity-action->http-request
                                    {:entities-type :trade-patterns
                                     :action        :get})
          [uri-regex-match]        (re-seq uri-regex-pattern uri)]
      (is http-req)
      (is (= method :get))
      (is (= uri uri-regex-match)))))

(comment
  (run-tests 'reason-alpha.web.service-api-test)

  (let [id                #uuid "32429cdf-99d6-4893-ae3a-891f8c22aec6"
        uri-regex-pattern (re-pattern
                           (str "^http://\\w*:?\\d*?/api/trade-patterns/" id "$"))]
    (re-seq uri-regex-pattern "http://localhost:3000/api/trade-patterns/32429cdf-99d6-4893-ae3a-891f8c22aec6"))

  (re-seq #"(?i)^http" "FOO BAR foo bar")
  )
