(ns reason-alpha.web.service-api
  (:require [ajax.core :as ajax]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.routes :as routes]
            [reitit.core :as reitit]))

(def base-uri "http://localhost:3000")

(def api-url "http://localhost:3000/api")

(defn- standard-headers
  "Adds:
    * The user token and format for API authorization"
  [_db]
  (let [auth-token "TODO_;-)"]
    [:Authorization (str "Token " auth-token)]))

(def router (routes/app-router))

(defn- resource
  ([type-kw]
   (resource type-kw nil))
  ([type-kw params]
   (str base-uri
        (:path (reitit/match-by-name
                router
                type-kw
                params)))))

(defn entity-action->http-request
  [{:keys [db entities-type action data on-success on-failure]
    :or   {on-success [:save-local entities-type]
           on-failure [:api-request-failure entities-type action]}}]
  (let [entity      (when data
                      (if (map? data)
                        data
                        (first data)))
        id-k        (when entity
                      (utils/id-key entity))
        ent-id      (when (and entity
                               (contains? entity id-k))
                      (get entity id-k))
        http-method (cond
                      (and (= :save action)
                           ent-id) :put

                      (and (= :save action)
                           (nil? ent-id)) :post

                      (and (= :delete action)) :delete

                      :else :get)
        single-entity-route (keyword (str (name entities-type)
                                          "/id"))
        uri                 (cond
                              (and (= http-method :get)
                                   ent-id) (resource single-entity-route {:id ent-id})

                              (= http-method :put) (resource single-entity-route {:id ent-id})

                              (= http-method :post) (resource entities-type)

                              (and (= http-method :delete) ;; Deleting a single entity
                                   ent-id) (resource single-entity-route {:id ent-id})

                              (and (= http-method :delete) ;; Deleting multiple entities
                                   (nil? ent-id)) (resource entities-type)

                              :else (resource entities-type))]
    {:http-xhrio (cond-> {:method          http-method
                          :headers         (standard-headers db)
                          :format          (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success      on-success
                          :on-failure      on-failure
                          :uri             uri}
                   (= :save action)
                   , (assoc :params data)

                   (and (= :delete action)
                        (nil? ent-id))
                   , (assoc :params {:delete-these data}))}))
