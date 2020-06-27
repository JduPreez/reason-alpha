(ns reason-alpha.web.service-api
  (:require [ajax.core :as ajax :refer [GET]]
            [clojure.string :as str]
            [cognitect.transit :as transit]
            [luminus-transit.time :as time]
            [reason-alpha.utils :as utils]
            [reason-alpha.web.routes :as routes]
            [reitit.core :as reitit]))

(def base-uri "http://localhost:3000/api")

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

(defn- resource [uri]
  (str/join "/" [base-uri uri]))

(defn- default-handler [response]
  (js/console.log (str response)))

(defn- error-handler [{:keys [status status-text]}]
  (js/console.log (str "something bad happened: " status " " status-text)))

(defn- as-transit [opts]
  (merge {:raw             false
          :format          :transit
          :response-format :transit
          :reader          (transit/reader :json time/time-deserialization-handlers)
          :writer          (transit/writer :json time/time-serialization-handlers)}
         opts))

(defn trade-patterns [& [handler]]
  (let [url (resource "trade-patterns")]
    (GET url (as-transit {:handler       (if handler handler default-handler)
                          :error-handler error-handler}))))

;; TODO: The idea here is that the :save event handler will call the save function,
;;       which in turn will inspect all the datasets under :data in the app-db
;;       and return a :http-xhrio map for each type of data to be saved (POST & PUT),
;;       that the event handler will then dispatch-n.
;; TODO: Move clj web.handler/routes to cljc & share with front-end. Lookup URL
;;       from routes by name.
;; TODO: Write unit tests for this!

(def router (routes/app-router))

(defn entity->command
  ([token [type-kw entity]]
   (let [type-nm (name type-kw)
         id      (utils/id-key type-kw) #_(keyword (str type-nm "/id"))]
     (if (contains? entity id)
       [:http-xhrio {:method          :put
                     :params          entity
                     :headers         (standard-headers token)
                     :format          (ajax/transit-request-format)
                     :response-format (ajax/transit-response-format)
                     :on-success      [:save-local type-kw]
                     :uri             (:path (reitit/match-by-name
                                              router
                                              type-kw
                                              {:user-id "NA"
                                               :id      (id entity)}))}]
       [:http-xhrio {:method          :post
                     :params          entity
                     :headers         (standard-headers token)
                     :format          (ajax/transit-request-format)
                     :response-format (ajax/transit-response-format)
                     :on-success      [:save-local type-kw]
                     :uri             (:path (reitit/match-by-name
                                              router
                                              (keyword (str type-nm "/*"))
                                              {:user-id "NA"}))}]))
   #_(->> (seq rentity)
        (group-by (fn [[attr _]] (namespace attr)))
        seq
        (map (fn [[ent-type attrs]]
               (let [id          (keyword (str ent-type "/id"))
                     ent-type-kw (keyword ent-type)
                     entity      (->> attrs
                                      (map (fn [[attr val]]
                                             [attr val]))
                                      flatten
                                      (apply hash-map))]
                 (if (contains? entity id)
                   [:http-xhrio {:method          :put
                                 :params          entity
                                 :headers         (standard-headers token)
                                 :format          (ajax/transit-request-format)
                                 :response-format (ajax/transit-response-format)
                                 :on-success      [:save-local ent-type-kw]
                                 :uri             (:path (reitit/match-by-name
                                                          router
                                                          (keyword ent-type)
                                                          {:user-id "NA"
                                                           :id      (id entity)}))}]
                   [:http-xhrio {:method          :post
                                 :params          entity
                                 :headers         (standard-headers token)
                                 :format          (ajax/transit-request-format)
                                 :response-format (ajax/transit-response-format)
                                 :on-success      [:save-local ent-type-kw]
                                 :uri             (:path (reitit/match-by-name
                                                          router
                                                          (keyword (str ent-type "/*"))
                                                          {:user-id "NA"}))}]))))
        vec))
  ([type-entity]
   (entity->command nil type-entity)))

(defn entities->commands
  ([entities token]
   (let [fn-ent->cmds (partial entity->command token)]
     (map fn-ent->cmds entities)))
  ([entities]
   (entities->commands entities nil)))

(comment
  (cljs.pprint/pprint 
   (let [rtr (routes/app-router)]
     #_(reitit/match-by-name rtr :trade-pattern {:user-id "kjkkj"})
     (rentities->commands {:trade-pattern/id             0
                         :trade-pattern/name          "Facebook"
                         :trade-pattern/owner-user-id 5
                         :user/user-name              "Frikkie"
                         :user/email                  "j@j.com"})))

   )


