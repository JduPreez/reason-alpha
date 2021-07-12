(ns reason-alpha.web.middleware
  (:require
   [muuntaja.middleware :refer [wrap-format wrap-params]]
   [reason-alpha.env :refer [defaults]]
   [reason-alpha.web.middleware.formats :as formats]
   [ring-ttl-session.core :refer [ttl-memory-store]]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
   [ring.middleware.flash :refer [wrap-flash]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.session.cookie :refer (cookie-store)]))

(defn wrap-cors [handler]
  (fn [request]
    (let [response (handler request)]
      (-> response
          (assoc-in [:headers "Access-Control-Allow-Origin"] "http://localhost:8700")
          (assoc-in [:headers "Access-Control-Allow-Headers"] "x-requested-with, content-type, x-csrf-token, Authorization")
          (assoc-in [:headers "Access-Control-Allow-Methods"] "*")))))

(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-flash
      wrap-cors
      (wrap-session {:cookie-attrs {:max-age 3600}
                     :store        (cookie-store {:key "ahY9poQuaghahc7I"})})
      (wrap-defaults
       (-> site-defaults
           (assoc-in [:security :anti-forgery] true)
           (dissoc :session)
           #_(assoc-in  [:session :store] (ttl-memory-store (* 60 30)))))))
