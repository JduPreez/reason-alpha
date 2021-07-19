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

#_(defn- endpoint
  "Concat any params to api-url separated by /"
  [& params]
  (str/join "/" (concat [api-url] params)))

#_(defn http-request [method resource db params]
  {:method          method
   :uri             (endpoint (name resource))
   :params          params
   :headers         (standard-headers db)
   :format          (ajax/transit-request-format)
   :response-format (ajax/transit-response-format)
   :on-success      [:save-local resource]})

#_(defn entity->command
  ([token [type entity]]
   (let [api-type (utils/entity-ns entity)
         id-k     (utils/id-key entity)]
     (if (contains? entity id-k)
       {:http-xhrio {:method          :put
                     :params          entity
                     :headers         (standard-headers token)
                     :format          (ajax/transit-request-format)
                     :response-format (ajax/transit-response-format)
                     :on-success      [:save-local type]
                     ;; TODO!!!: Prefix correct server side base URL here!
                     :uri             (resource (keyword api-type) {:id (id-k entity)})}}
       {:http-xhrio {:method          :post
                     :params          entity
                     :headers         (standard-headers token)
                     :format          (ajax/transit-request-format)
                     :response-format (ajax/transit-response-format)
                     :on-success      [:save-local type]
                     :uri             (resource (keyword (str api-type "/*")))
                     }})))
  ([type-entity]
   (entity->command nil type-entity)))

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
        ent-id      (when (contains? entity id-k)
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

                              (and (= http-method :delete)
                                   ent-id) (resource single-entity-route {:id ent-id})

                              (and (= http-method :delete)
                                   (nil? ent-id)) (resource entities-type)

                              :else (resource entities-type))]
    {:http-xhrio (cond-> {:method          http-method
                          :headers         (standard-headers db)
                          :format          (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success      on-success
                          :on-failure      on-failure
                          :uri             uri}
                   (= :save action) (assoc :params data))}))

#_(defn entities->commands
  ([entities token]
   (let [fn-ent->cmds (partial entity->command token)]
     (map fn-ent->cmds entities)))
  ([entities]
   (entities->commands entities nil)))
