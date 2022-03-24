(ns reason-alpha.infrastructure.auth
  (:require [buddy.core.keys :as keys] ;; You need to use [buddy/buddy-core "1.5.0-SNAPSHOT"]
            [buddy.sign.jwt :as jwt]
            [cheshire.core :as json]
            [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [malli.core :as m]
            [outpace.config :refer [defconfig]]
            [reason-alpha.model.accounts :as accounts]
            [reason-alpha.model.common :as common]
            [clojure.set :as set]))

(defconfig tenant-id)

(def *public-key (delay (-> "jwt.key"
                            slurp
                            keys/str->public-key)))

(defn token-data [token & [public-key]]
  (try
    (let [pk                           (or public-key @*public-key)
          {:keys [userId] :as content} (jwt/unsign token pk {:alg :rs256})]
      (merge content {:is-valid? (boolean userId)}))
    (catch Throwable t
      {:error     true
       :is-valid? false
       :details   (ex-data t)})))

(defn tokens [{:keys [cookies] :as request}]
  {:access-token  (get-in cookies [(str "access." tenant-id) :value])
   :user-token    (get-in cookies [(str "id." tenant-id) :value])
   :refresh-token (get-in cookies [(str "refresh." tenant-id) :value])})

(m/=> account
      [:=> [:cat :any]
       accounts/Account])

;; TODO: This must be cached
(defn account [request]
  (let [{:keys [user-token] :as tokens}              (tokens request)
        {:keys [userUuid email username name image]} (token-data user-token)]
    {:account/user-id   userUuid
     :account/user-name username
     :account/profile   {:profile/email email
                         :profile/name  name
                         :profile/image image}}))

(defn authorize
  [{:keys [fn-get-account crud role account-id-key]} entities & [current-ents-owners]]
    (if (or (not (seq entities))
            (#{:system :admin} role))
      entities
      (let [{acc-id :account/id} (fn-get-account)
            acc-id-k             (if account-id-key
                                   account-id-key
                                   (->> entities
                                        first
                                        keys
                                        (some #(when (= "account-id" (name %))
                                                 %))))
            authz-ents           (if acc-id-k
                                   (filterv #(= (get % acc-id-k) acc-id) entities)
                                   [])]
        (clojure.pprint/pprint {::authorize {:ACC-ID         acc-id
                                             :ENTS           entities
                                             :AUTHZ-ENTS     authz-ents
                                             :ACCOUNT-ID-KEY account-id-key
                                             :ACC-ID-KEY     acc-id-k}})
        authz-ents)))

(comment

  (let [account-id (java.util.UUID/randomUUID)
        ents       (->> (range 1 11)
                        (map
                         (fn [n]
                           {:position/creation-id         (java.util.UUID/randomUUID)
                            :position/id                  (java.util.UUID/randomUUID)
                            :position/status              (if (even? n) :open :closed)
                            :position/instrument-id       (java.util.UUID/randomUUID)
                            :position/holding-position-id (java.util.UUID/randomUUID)
                            :position/trade-pattern-id    (java.util.UUID/randomUUID)
                            :position/account-id          (if (even? n)
                                                            account-id
                                                            (java.util.UUID/randomUUID))})))
        authz-ents (authorize {:fn-get-account (constantly {:account/id account-id})
                               :crud           :read
                               :role           :member
                               :entities       ents})]
    authz-ents)

  (let [account-id (java.util.UUID/randomUUID)
        ents       (->> (range 1 11)
                        (map
                         (fn [n]
                           (if (even? n)
                             [account-id]
                             [(java.util.UUID/randomUUID)]))))
        authz-ents (authorize {:fn-get-account (constantly {:account/id account-id})
                               :crud           :read
                               :role           :member
                               :entities       ents
                               :account-id-key 0})]
    authz-ents)


  (seq [])

  (not nil)



  {:tenantId    "7n8w5rqb",
   :email       "jacquesdpz@zoho.com",
   :confirmedAt nil,
   :updatedAt   "2022-02-14T08:28:11.620Z",
   :sessionId   "a8295c82-b5c7-4add-b057-f44b1a5e8e91",
   :name        "Test1",
   :exp         1645432091,
   :username    "fantastic-sun-69l94",
   :mode        "test",
   :createdAt   "2022-02-14T08:28:11.620Z",
   :userUuid    "45355491-519b-4839-bd69-e0064e2d259b",
   :isConfirmed false,
   :userId      5,
   :image
   "https://res.cloudinary.com/component/image/upload/avatars/avatar-plain-8.png",
   :iat         1644827291,
   :data        {}}

  )
