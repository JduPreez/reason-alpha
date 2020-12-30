(ns reason-alpha.web.service-api
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.routes :as routes]
            [reitit.core :as reitit]))

(def base-uri "http://localhost:3000")

(def api-url "http://localhost:3000/api")

;; TODO: Tidy this function up
(defn- standard-headers
  "Adds:
    * The user token and format for API authorization
    * CSRF token"
  [token]
  (let [headers [:x-csrf-token (when (exists? js/csrfToken)
                                 js/csrfToken)]]
    (if token
      (conj headers :Authorization (str "Token " token))
      headers)))

#_(defn- default-handler [response]
  (js/console.log (str response)))

#_(defn- error-handler [{:keys [status status-text]}]
  (js/console.log (str "something bad happened: " status " " status-text)))

#_(defn- as-transit [opts]
  (merge {:raw             false
          :format          :transit
          :response-format :transit
          :reader          (transit/reader :json time/time-deserialization-handlers)
          :writer          (transit/writer :json time/time-serialization-handlers)}
         opts))

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

(defn entity->command
  ([token [type entity]]
   (let [api-type (utils/entity-ns entity)
         id-k     (utils/id-key entity) #_ (keyword (str type-nm "/id"))]
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

(defn entities->commands
  ([entities token]
   (let [fn-ent->cmds (partial entity->command token)]
     (map fn-ent->cmds entities)))
  ([entities]
   (entities->commands entities nil)))

(defn- endpoint
  "Concat any params to api-url separated by /"
  [& params]
  (str/join "/" (concat [api-url] params)))

(defn http-request [method resource db params]
  {:method          method
   :uri             (endpoint (name resource))
   :params          params
   :headers         (standard-headers db)
   :format          (ajax/transit-request-format)
   :response-format (ajax/transit-response-format)
   :on-success      [:save-local resource]})
