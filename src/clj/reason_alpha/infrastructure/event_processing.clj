(ns reason-alpha.infrastructure.event-processing
  (:require [clojure.core.async :as as]
            [reason-alpha.utils :as utils]))

(def *topic-fns (atom {}))

(def message-chan (as/chan))

(defn- filter-topic
  [{msg-type :msg/type :as msg}]
  ;; Get the custom topic filter, otherwise default to filtering on
  ;; the value of `msg-type`
  (println "filter-topic")
  (if-let [fn-topic (get @*topic-fns msg-type)]
    (do
      (println "filter-topic fn found " msg)
      (fn-topic msg))
    #_else
    (do
      (println "filter-topic fn NOT found")
      msg-type)))

(def message-pub (as/pub message-chan filter-topic (constantly
                                                     (as/sliding-buffer 10))))

(defn register-topic-fn
  [msg-type f]
  (swap! *topic-fns assoc msg-type f))

(defn send-msg [msg & [result-topic]]
  ;; Message has the same structure as how the front-end
  ;; sends messages.
  ;; From the message structure we can figure out which `module.service/command-or-query`
  ;; to forward the message to.

  ;; Return result message contains original in message & the result out message.
  ;; Once it's received the result message it's interested in, then the `sub` channel must
  ;; be closed (or timed-out).
  (if result-topic
    ;; TODO: `take` doesn't automatically close the channel
    (let [c (as/take 1 (as/chan 1))
          s (as/sub message-pub result-topic c)]
      (println "AAA")
      (as/>!! message-chan msg)
      (println "BBB")
      [c s])
    #_else
    (do
      (println "Sending message without result sub")
      (utils/ignore
       (as/>!! message-chan msg)))))

(defn receive-msg [topic]
  (let [c (as/chan)
        s (as/sub message-pub topic c)]
    c))

(defn stop-receive-msg [topic chanl]
  (as/unsub message-pub topic chanl))

(comment

  (let [c (receive-msg :market-data/get-equity-prices)]
    (as/go-loop []
      (let [{:keys [msg/value] :as m} (as/<! c)]
        (println value)
        (send-msg {:msg/type   :market-data/get-equity-prices-result
                   :msg/value  (str "Received '" value "' and this is the reply")
                   :msg/id     (utils/new-uuid)
                   :msg/source m})
        (recur))))

  (register-topic-fn :market-data/get-equity-prices-result
                     (fn [{msg-type          :msg/type
                           _msg-val          :msg/value
                           _msg-id           :msg/id
                           {src-mid :msg/id} :msg/source}]
                       (println "Topic fn: " [msg-type src-mid])
                       [msg-type src-mid]))

  (let [mid   "Test1234"
        t     [:market-data/get-equity-prices-result mid]
        [c s] (send-msg
               {:msg/type  :market-data/get-equity-prices
                :msg/value "Hello Pub/Sub :-)"
                :msg/id    mid}
               t)]
    (as/go
      (let [_   (println "Waiting for result")
            res (as/<! c)]
        (println "RESULT!!!: " res)
        (stop-receive-msg t c)))
    #_(as/close! c)
    #_(as/unsub message-pub t c))

  (let [mid "Test1234"]
    (send-msg
     {:msg/type  :market-data/get-equity-prices
      :msg/value "Hello Pub/Sub :-)"
      :msg/id    mid})
    #_(as/close! c)
    #_(as/unsub message-pub t c))



  ;;(send-msg {:market } :market-data/get-equit-prices-result)


  ;; Each module has it's own `model` ns with a configured system,
  ;; and uses `model/handlers` to 

  {:module-x (as/chan)
   :module-y (as/chan)
   :modeul-z (as/chan)}

  )
