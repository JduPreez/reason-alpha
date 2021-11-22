(ns reason-alpha.web.middleware
  (:require
   [muuntaja.middleware :refer [wrap-format wrap-params]]
   [reason-alpha.env :refer [defaults]]
   [reason-alpha.web.middleware.formats :as formats]
   [ring-ttl-session.core :refer [ttl-memory-store]]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [ring.middleware.defaults :refer [site-defaults api-defaults wrap-defaults]]
   [ring.middleware.flash :refer [wrap-flash]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.session.cookie :refer (cookie-store)]
   [ring.util.request :as req-util]))

(defn wrap-cors [handler]
  (fn [request]
    (let [response (handler request)]
      #_(clojure.pprint/pprint {::wrap-cors {:req request
                                             :res response}})
      (-> response
          (assoc-in [:headers "Access-Control-Allow-Origin"] "http://localhost:8700")
          (assoc-in [:headers "Access-Control-Allow-Headers"] "X-Requested-With,Content-Type,x-csrf-token,Authorization,Origin")
          (assoc-in [:headers "Access-Control-Allow-Methods"] "*")))))

(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn wrap-body-str [handler]
  (fn [request]
    (let [body-str (ring.util.request/body-string request)]
      (clojure.pprint/pprint {::wrap-body-string request})
      (handler (assoc request :body (java.io.StringReader. body-str))))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      ;;wrap-body-str
      wrap-flash
      wrap-cors
      (wrap-session {:cookie-attrs {:same-site :none}})
      (wrap-defaults
       (-> api-defaults
           (dissoc :session)))))
