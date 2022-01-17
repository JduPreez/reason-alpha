(ns reason-alpha.web.api-client
  (:require [re-frame.core :as rf]
            [reason-alpha.web.handlers :as handlers]
            [taoensso.sente :as sente]))

(def *router (atom nil))

(def *ch-chsk (atom nil))

(def *chsk-send! (atom nil))

(def *chsk-state (atom nil))

(def config {:type     :auto
             :packer   :edn
             :protocol :http
             :host     "localhost"
             :port     5000})

(defn chsk-send! [event & [{:keys [on-success on-failure]}]]
  (@*chsk-send! event 60000
   (fn [reply]
     (let [cb-success? (sente/cb-success? reply)]
       (cond
         (and cb-success? on-success)
         , (rf/dispatch (conj on-success reply))

         (and (not cb-success?) on-failure)
         , (rf/dispatch (conj on-failure)))))))

(defn state-watcher [_key _atom _old-state new-state]
  (.warn js/console "New state" new-state))

(defn create-client! []
  (let [{:keys [ch-recv send-fn state]} (sente/make-channel-socket-client! "/chsk" nil config)]
    (reset! *ch-chsk ch-recv)
    (reset! *chsk-send! send-fn)
    (add-watch state :state-watcher state-watcher)))

(defn stop-router! []
  (when-let [stop-f @*router] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! *router (sente/start-client-chsk-router! @*ch-chsk handlers/event-msg-handler)))

(defn start! []
  (create-client!)
  (start-router!))
