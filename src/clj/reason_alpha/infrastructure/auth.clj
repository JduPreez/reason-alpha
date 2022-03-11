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
            [reason-alpha.model.common :as common]))

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

(comment

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
