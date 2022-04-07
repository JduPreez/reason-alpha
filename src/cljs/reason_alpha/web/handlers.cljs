(ns reason-alpha.web.handlers
  (:require [re-frame.core :as rf]))

(defn log [message data]
  (.log js/console message (.stringify js/JSON (clj->js data))))

(defmulti -event-msg-handler :id)

(defn event-msg-handler
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler :default
  [{:keys [id event ?data]}]
  (cljs.pprint/pprint ["Unhandled event:" (js->clj event)]))

(defmethod -event-msg-handler :chsk/recv
  [{:keys [id event ?data] :as ev-msg}]
  (cljs.pprint/pprint {"Push event from server:" ?data})

  (if-let [[evt-id data] ?data]
    (rf/dispatch
     (->> [evt-id data]
          (remove nil?)
          vec))))
