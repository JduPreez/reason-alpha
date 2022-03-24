(ns reason-alpha.infrastructure.server
  (:require [clojure.core.async :as async  :refer (<! <!! >! >!! put! chan go go-loop)]
            [clojure.pprint :as pprint]
            [compojure.core :as comp :refer (defroutes routes GET POST ANY)]
            [compojure.route :as route]
            [org.httpkit.server :as http-kit]
            [outpace.config :refer [defconfig]]
            [reason-alpha.infrastructure.auth :as auth]
            [reason-alpha.model.common :as common]
            [ring.middleware.anti-forgery :as anti-forgery]
            [ring.middleware.cors :as ring-cors]
            [ring.middleware.defaults :as ring-defaults]
            [taoensso.sente :as sente]
            [taoensso.sente.packers.transit :as sente-transit]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]))

(defconfig allowed-origins)

;; (timbre/set-level! :trace) ; Uncomment for more logging
(reset! sente/debug-mode?_ true) ; Uncomment for extra debug info

(defn authenticated? [{:keys [request-method compojure/route] :as request}]
  (let [{:keys [is-valid? error userUuid email]} (-> request
                                                     auth/tokens
                                                     :user-token
                                                     auth/token-data)]
    (debugf "User autenticated? %s (%s): %b" userUuid email is-valid?)

    (or (= request-method :options)
        is-valid?)))

(let [;; Serialization format, must use same val for client + server:
      packer :edn ; Default packer, a good choice in most cases
      ;; (sente-transit/get-transit-packer) ; Needs Transit dep

      chsk-server
      (sente/make-channel-socket-server!
       (get-sch-adapter) {:packer         packer
                          :csrf-token-fn  nil
                          :authorized?-fn authenticated?})

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
  [{{:keys [fn-save-account!]} :account-svc}
   {:keys [request-method] :as request}]
  (when (not= request-method :options)
    (let [{:keys [is-valid? error userUuid email]} (-> request
                                                       auth/tokens
                                                       :user-token
                                                       auth/token-data)]

      (debugf "Verified login of user %s (%s): %b" userUuid email is-valid?)

      (if is-valid?
        (let [acc (auth/account request)]
          (fn-save-account! acc)
          {:status 200
           :body   {:result "Access granted"}})
        (do
          (debugf "Error verifying login %s" error)
          {:status 401
           :body   {:result "Access denied"
                    :reason (when error "Error")}})))))

(defn wrap-cors [handler allow-origin]
  (fn [{:keys [request-method] :as request}]
    (let [response (handler request)
          response (cond-> (-> response
                               (assoc-in [:headers "Access-Control-Allow-Origin"] allow-origin)
                               (assoc-in [:headers "Access-Control-Allow-Credentials"] "true")
                               (assoc-in [:headers "Access-Control-Allow-Headers"] "x-requested-with, content-type")
                               (assoc-in [:headers "Access-Control-Allow-Methods"] "*"))
                     (#{:options} request-method) (assoc :status 200))]
      response)))

(defn ring-routes [conf]
  (routes
   (GET  "/chsk"  ring-req (ring-ajax-get-or-ws-handshake ring-req))
   (POST "/chsk"  ring-req (ring-ajax-post ring-req))
   (POST "/login" ring-req (login-handler conf ring-req))))

(defn main-ring-handler [conf]
  (-> (ring-routes conf)
      (ring-defaults/wrap-defaults ring-defaults/api-defaults)
      ring.middleware.session/wrap-session
      (wrap-cors allowed-origins)))

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

  (when (not= :chsk/ws-ping id)
    (clojure.pprint/pprint {::server-event-msg-handler ev-msg}))

  (let [fun     (get handlers id)
        account (auth/account ring-req)
        data    (or ?data {})]
      ;;future
        (if fun
          (binding [common/*context* {:user-account account
                                      :send-message #(chsk-send! uid %)}]
            (let [_      (debugf "Event handler found: %s" id) ;; Log before calling `fun` might throw exception
                  result (fun data)]
              (when ?reply-fn
                (?reply-fn result))))
          (do
            (when (not= :chsk/ws-ping id)
              (debugf "Unhandled event: %s" event))

            (when ?reply-fn
              (?reply-fn {:umatched-event-as-echoed-from-server event})))))
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
(defn start-web-server! [{:keys [port] :as conf}]
  (stop-web-server!)
  (let [port         (or port 0)              ; 0 => Choose any available port
        ring-handler (main-ring-handler conf) #_ (var main-ring-handler)

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

(defn start! [{:keys [handlers]
               :as   conf}]
  (start-router! handlers)
  (start-web-server! conf)
  #_(let [*ws (start-web-server! 5000)]
    (start-example-broadcaster!)
    *ws))
