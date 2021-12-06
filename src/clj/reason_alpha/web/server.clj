(ns reason-alpha.web.server
  (:require [clojure.core.async :as async  :refer (<! <!! >! >!! put! chan go go-loop)]
            [compojure.core     :as comp :refer (defroutes GET POST context)]
            [compojure.route    :as route]
            [org.httpkit.server :as http-kit]
            [ring.middleware.anti-forgery :as anti-forgery]
            [ring.middleware.defaults :as ring-defaults]
            [ring.middleware.cors :as ring-cors]
            [taoensso.sente     :as sente]
            [taoensso.sente.packers.transit :as sente-transit]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
            [taoensso.timbre    :as timbre :refer (tracef debugf infof warnf errorf)]
            [clojure.pprint :as pprint]))

;; (timbre/set-level! :trace) ; Uncomment for more logging
(reset! sente/debug-mode?_ true) ; Uncomment for extra debug info

(let [;; Serialization format, must use same val for client + server:
      packer :edn ; Default packer, a good choice in most cases
      ;; (sente-transit/get-transit-packer) ; Needs Transit dep

      chsk-server
      (sente/make-channel-socket-server!
       (get-sch-adapter) {:packer packer})

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

;;;; Ring handlers
(defroutes ring-routes
  (context "/api" []
           (GET  "/chsk"  ring-req (ring-ajax-get-or-ws-handshake ring-req))
           (POST "/chsk"  ring-req (ring-ajax-post                ring-req)))
  (route/resources "/") ; Static files, notably public/main.js (our cljs target)
  (route/not-found "<h1>Page not found</h1>"))

(def main-ring-handler
  (def main-ring-handler
    (-> ring-routes
        (ring-defaults/wrap-defaults ring-defaults/site-defaults)
        (ring-cors/wrap-cors :access-control-allow-origin [#".*"]))))

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
      (<! (async/timeout 10000))
      (when @broadcast-enabled?_ (broadcast! i))
      (recur (inc i)))))

;;;; Sente event handlers

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg) ; Handle event-msgs on a single thread
  ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
  )

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-server event}))))

(defmethod -event-msg-handler
  :trade-patterns/get
  [event]
  (pprint/pprint {:trade-patterns/get event}))

;;;; Sente event router (our `event-msg-handler` loop)
(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-router! []
  (stop-router!)
  (reset! router_
    (sente/start-server-chsk-router!
      ch-chsk event-msg-handler)))

;;;; Init stuff
(defonce web-server (atom nil)) ; (fn stop [])
(defn stop-web-server! [] (when-let [stop-fn @web-server] (stop-fn)))
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
    (reset! web-server stop-fn)))

(defn stop!  [] (stop-router!)  (stop-web-server!))
(defn start! [] (start-router!) (start-web-server!) (start-example-broadcaster!))
