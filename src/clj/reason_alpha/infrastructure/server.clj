(ns reason-alpha.infrastructure.server
  (:require [clojure.core.async :as async  :refer (<! <!! >! >!! put! chan go go-loop)]
            [clojure.pprint :as pprint]
            [compojure.core     :as comp :refer (defroutes GET POST ANY)]
            [compojure.route    :as route]
            [org.httpkit.server :as http-kit]
            [ring.middleware.anti-forgery :as anti-forgery]
            [ring.middleware.cors :as ring-cors]
            [ring.middleware.defaults :as ring-defaults]
            [taoensso.sente     :as sente]
            [taoensso.sente.packers.transit :as sente-transit]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
            [taoensso.timbre    :as timbre :refer (tracef debugf infof warnf errorf)]
            [reason-alpha.infrastructure.auth :as auth]))

;; (timbre/set-level! :trace) ; Uncomment for more logging
(reset! sente/debug-mode?_ true) ; Uncomment for extra debug info

(defn authenticated? [{:keys [request-method] :as _request}]
  #_(pprint/pprint {::authenticated? _request})
  #_(when (not= request-method :options)
      false)
  nil)

(let [;; Serialization format, must use same val for client + server:
      packer :edn ; Default packer, a good choice in most cases
      ;; (sente-transit/get-transit-packer) ; Needs Transit dep

      chsk-server
      (sente/make-channel-socket-server!
       (get-sch-adapter) {:packer             packer
                          :csrf-token-fn      nil
                          #_#_:authorized?-fn authenticated?})

      {:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      chsk-server]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

;; We can watch this atom for changes if we like
(add-watch connected-uids :connected-uids
  (fn [_ _ old new]
    (when (not= old new)
      (infof "Connected uids change: %s" new))))

(defn login-handler
  "Here's where you'll add your server-side login/auth procedure (Friend, etc.).
  In our simplified example we'll just always successfully authenticate the user
  with whatever user-id they provided in the auth request."
  [{:keys [request-method] :as request}]
  (when (not= request-method :options)
    (let [{:keys [user-token] :as tokens}          (auth/tokens request)
          {:keys [is-valid? error userUuid email]} (auth/verify-token user-token)]
      (clojure.pprint/pprint {::login-handler {:request request
                                               :tokens  tokens}})
      (debugf "Verified login of user %s (%s): %b" userUuid email is-valid?)

      (if is-valid?
        {:status 200
         :body   {:result "Access granted"}}
        (do
          (debugf "Error verifying login %s" userUuid email error)
          {:status 401
           :body   {:result "Access denied"
                    :reason (when error "Error")}})))))

;;;; Ring handlers
(defroutes ring-routes
  (GET  "/chsk"  ring-req (ring-ajax-get-or-ws-handshake ring-req))
  (POST "/chsk"  ring-req (ring-ajax-post                ring-req))
  (POST "/login" ring-req (login-handler                 ring-req))
  #_(route/not-found "<h1>Page not found</h1>"))

(defn wrap-cors [handler allowed-origins]
  (fn [{:keys [request-method] :as request}]
    (let [response (handler request)
          response (cond-> (-> response
                               (assoc-in [:headers "Access-Control-Allow-Origin"] allowed-origins)
                               (assoc-in [:headers "Access-Control-Allow-Credentials"] "true")
                               (assoc-in [:headers "Access-Control-Allow-Headers"] "x-requested-with, content-type")
                               (assoc-in [:headers "Access-Control-Allow-Methods"] "*"))
                     (#{:options} request-method) (assoc :status 200))]
      (clojure.pprint/pprint {::wrap-cors response})
      response)))

(def main-ring-handler
  (-> ring-routes
      (ring-defaults/wrap-defaults ring-defaults/api-defaults)
      ring.middleware.session/wrap-session
      (wrap-cors "http://localhost:8700")))

(defonce broadcast-enabled?_ (atom true))

(defn start-example-broadcaster!
  "As an example of server>user async pushes, setup a loop to broadcast an
  event to all connected users every 10 seconds"
  []
  (let [broadcast!
        (fn [i]
          (let [uids (:any @connected-uids)]
            (debugf "Broadcasting server>user: %s uids" (count uids))
            (doseq [uid uids]
              (chsk-send! uid
                [:some/broadcast
                 {:what-is-this "An async broadcast pushed from server"
                  :how-often "Every 10 seconds"
                  :to-whom uid
                  :i i}]))))]

    (go-loop [i 0]
      (<! (async/timeout 60000))
      (when @broadcast-enabled?_ (broadcast! i))
      (recur (inc i)))))

(defn server-event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [handlers {:as ev-msg :keys [id ?data event ?reply-fn ring-req uid]}]
  (pprint/pprint {::server-event-msg-handler ring-req})
  (let [fun (get handlers id)]
    (if fun
      (do
        (debugf "Event handler found: %s" id)
        (if ?reply-fn
          (?reply-fn (fun ?data))
          (fun ?data)))
      (do
        (debugf "Unhandled event: %s" event)
        (when ?reply-fn
          (?reply-fn {:umatched-event-as-echoed-from-server event}))) ))
                                        ; Handle event-msgs on a single thread
  ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
  )

;;;; Sente event router (our `event-msg-handler` loop)
(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-router! [handlers]
  (stop-router!)
  (reset! router_
    (sente/start-server-chsk-router!
     ch-chsk #(server-event-msg-handler handlers %))))

;;;; Init stuff
(defonce *web-server (atom nil)) ; (fn stop [])
(defn stop-web-server! [] (when-let [stop-fn @*web-server] (stop-fn)))
(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [port         (or port 0) ; 0 => Choose any available port
        ring-handler (var main-ring-handler)

        [port stop-fn] (let [stop-fn (http-kit/run-server
                                      ring-handler
                                      {:port port})]
                         [(:local-port (meta stop-fn))
                          (fn [] (stop-fn :timeout 100))])
        uri            (format "http://localhost:%s/" port)]

    (infof "Web server is running at `%s`" uri)
    (reset! *web-server stop-fn)
    *web-server))

(defn stop!  []
  (stop-router!)
  (stop-web-server!))

(defn start! [handlers]
  (start-router! handlers)
  (let [*ws (start-web-server! 5000)]
    (start-example-broadcaster!)
    *ws))
