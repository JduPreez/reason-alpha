(ns reason-alpha.infrastructure.message-processing
  (:require [clojure.core.async :as as]
            [promesa.core :as p]
            [reason-alpha.model.utils :as mutils]
            [reason-alpha.utils :as utils]))

(def *topic-fns (atom {}))

(def message-chan (as/chan))

(defn- filter-topic
  [{msg-type :msg/type :as msg}]
  ;; Get the custom topic filter, otherwise default to filtering on
  ;; the value of `msg-type`
  (clojure.pprint/pprint {:FILTER-TOPIC msg})
  (if-let [fn-topic (get @*topic-fns msg-type)]
    (fn-topic msg)
    #_else
    msg-type))

(def message-pub (as/pub message-chan filter-topic (constantly
                                                    (as/sliding-buffer 10))))

(defn register-topic-fn
  [msg-type f]
  (swap! *topic-fns assoc msg-type f))

(defn stop-receive-msg [chnl]
  ;; We previously also `unsub`scribed, but for some unknown reason
  ;; this completely stopped the topic publication completely, not
  ;; only removed the specific channel from the topic.
  ;; (as/unsub message-pub result-topic c)
  (as/close! chnl))

(defn send-msg [{id :msg/id :as msg} & {:keys [result-topic fn-receive-msg fn-error]}]
  ;; Message has the same structure as how the front-end
  ;; sends messages.
  ;; From the message structure we can figure out which `module.service/command-or-query`
  ;; to forward the message to.

  ;; Return result message contains original in message & the result out message.
  ;; Once it's received the result message it's interested in, then the `sub` channel must
  ;; be closed (or timed-out).
  (let [msg (assoc msg :msg/id (or id (utils/new-id)))]
    (if result-topic
      (let [c (as/chan)]
        (as/sub message-pub result-topic c)
        (as/go
          (try
            (let [r (as/<! c)]
              (fn-receive-msg r))
            (catch Exception e
              (if fn-error
                (fn-error e)
                (clojure.pprint/pprint (ex-data e))))
            (finally
              (stop-receive-msg c))))
        (as/>!! message-chan msg))
      #_else
      (utils/ignore
       (as/>!! message-chan msg)))))

(defn receive-msg
  [msg-type-topic]
  (let [c (as/chan)
        s (as/sub message-pub msg-type-topic c)]
    c))

(defn start-receive-msg
  [msg-type-topic & {:keys [result-msg-type-topic fn-receive-msg]}]
  (let [chnl (receive-msg msg-type-topic)]
    (as/go-loop []
      (let [{v   :msg/value
             t   :msg/type
             :as m} (as/<! chnl)
            res-v   (fn-receive-msg v)]
        (when result-msg-type-topic
          (as/>! message-chan
                 {:msg/type   result-msg-type-topic
                  :msg/value  res-v
                  :msg/id     (utils/new-id)
                  :msg/source m}))
        (recur)))
    chnl)
  #_(as/go-loop []
      (let [m (as/<! chanl)]
      (fn-receive-msg m)
      (recur))))

(comment

  #_(let [c (receive-msg :market-data/get-equity-prices)]
    (as/go-loop []
      (let [{:keys [msg/value] :as m} (as/<! c)]
        (println value)
        (send-msg {:msg/type   :market-data/get-equity-prices-result
                   :msg/value  (str "Received '" value "' and this is the reply")
                   :msg/id     (utils/new-id)
                   :msg/source m})
        (recur))))

  (start-receive-msg :market-data/get-equity-prices
                     :result-msg-type-topic :market-data/get-equity-prices-result
                     :fn-receive-msg #(do
                                        (clojure.pprint/pprint
                                         {:market-data/get-equity-prices %})
                                        (str "Received '" % "' and this is the reply")))

  #_(start-receive-msg :market-data/get-fx-prices
                     :result-msg-type-topic :market-data/get-fx-prices-result
                     :fn-receive-msg #(do
                                        (clojure.pprint/pprint
                                         {:market-data/get-fx-prices %})
                                        "Welcome to Pub/Sub!!!!"))

  ;; (register-topic-fn :market-data/get-equity-prices-result
  ;;                      (fn [{msg-type          :msg/type
  ;;                            _msg-val          :msg/value
  ;;                            _msg-id           :msg/id
  ;;                            {src-mid :msg/id} :msg/source}]
  ;;                      (println "Topic fn: " [msg-type src-mid])
  ;;                      [msg-type src-mid]))

  (let [mid-get-eq-prs       (utils/new-id)
        t                    :market-data/get-equity-prices-result #_mid-get-eq-prs
        *get-eq-prs-res      (p/deferred)
        _                    (send-msg
                              {:msg/type  :market-data/get-equity-prices
                               :msg/value "Hello share prices Pub/Sub"
                               :msg/id    mid-get-eq-prs}
                              :result-topic t
                              :fn-receive-msg #(do
                                                 (println "$$$$$$$$$$$$$$$$$$$$$$$$")
                                                 (p/resolve! *get-eq-prs-res %)))
        #_#_mid-get-fx-prs   (utils/new-id)
        #_#_res-t-get-fx-prs [:market-data/get-fx-prices-result mid-get-fx-prs]
        #_#_*get-fx-prs-res  (p/deferred)
        #_#__                (send-msg
                              {:msg/type  :market-data/get-fx-prices
                               :msg/value "Hello FX Pub/Sub"
                               :msg/id    mid-get-fx-prs}
                              :result-topic res-t-get-fx-prs
                              :fn-receive-msg #(p/resolve! *get-fx-prs-res %))]
    @(-> *get-eq-prs-res
         (p/timeout 9000)
         (p/then #(p/resolved %))
         (p/catch #(println "Timeout" %)))
    #_@(-> [*get-eq-prs-res *get-fx-prs-res]
         p/all
         (p/then (fn [[get-eq-prs-res get-fx-prs-res]]
                   (clojure.pprint/pprint {:$$$$$$->>-1 get-eq-prs-res
                                           :$$$$$$->>-2 get-fx-prs-res})))))


  ;; Each module has it's own `model` ns with a configured system,
  ;; and uses `model/handlers` to 

  {:module-x (as/chan)
   :module-y (as/chan)
   :modeul-z (as/chan)})
