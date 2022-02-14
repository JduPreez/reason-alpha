(ns reason-alpha.infrastructure.auth
  (:require [buddy.core.keys :as keys] ;; You need to use [buddy/buddy-core "1.5.0-SNAPSHOT"]
            [buddy.sign.jwt :as jwt]
            [cheshire.core :as json]
            [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [outpace.config :refer [defconfig]]))

(defconfig tenant-id)

(def *public-key (delay (-> "jwt.key"
                            slurp
                            keys/str->public-key)))

(defn decode-b64 [str] (String. (b64/decode (.getBytes str))))
(defn parse-json [s]
  ;; beware, I got some tokens from AWS Cognito where the last '}' was missing in the payload
  (let [clean-str (if (str/ends-with? s "}") s (str s "}"))]
    (json/parse-string clean-str keyword)))

(defn decode [token]
  (let [[header payload _] (clojure.string/split token #"\.")]
    {:header  (parse-json (decode-b64 header))
     :payload (parse-json (decode-b64 payload))}))

(defn verify-token [token & [public-key]]
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

(comment

  (boolean 0)

  (let [pk "-----BEGIN PUBLIC KEY-----\nMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAoxw+1QxhmQT0HVvPRCJE\n19C2wItKHXfFiAHFW4M5Fc3zeaBss84xuoIXZthpn7hvUn9gW99FkNHtoIF1F0yy\nUT3l91Tf7ZxxAnlCtUUOBincctf/3gyx+5KckVVRU5OT0q/m+HfLhvKGpYsrFhto\n/fnZgS0k/ZXHoygD6/5BJqkbtZVCo09h55WwvsoGBtXhSpl/0+FXVj0SEcjfVP6M\n6xHSnd6ggWOVjGrOu0cxPbhzsaBHX3mftIfL8BISVhchKFI+hbojHTsX1xuwB80/\nCz1FdK7FTWld4HhigyAtkIlU+8IlODfv+PywErOMjr1nzOil9EOzN4lWoILL5a1v\nqs7GpJJyRntbeAv2MIup7AwmmI7qhNS61cXS3Jm4URpRgBNijK2XnldbiQrMoWYo\n6rUq0GZzT82k3fpOJle/eij3ePzodqxzEBAcXA1UDWjMiCYr9ygu8gJkXnkIqm2k\n9Y28pswLoVCNFHeoonqAsSsQmjDmg4mHRo0PWQjQ9pd120SS4EhY0VJJWJUu5OCV\nuPUnqFw1CJ8GQP2VuD8YQvsnDsImNx+XyUDccpc3Nzh2drgs926kqxTiVwgsP2ld\nQKJUKcpYyRnhPpuP8mVAEm6SX+BvBQPr5bogwvEpxvvd0vp02lALD2c6C+WmkquV\ndU/g2NYxtHap02eQjc4JsIECAwEAAQ==\n-----END PUBLIC KEY-----"]
    (keys/str->public-key pk))

  @*public-key

  {:tenantId "7n8w5rqb",
   :email "jacquesdpz@zoho.com",
   :confirmedAt nil,
   :updatedAt "2022-02-14T08:28:11.620Z",
   :sessionId "a8295c82-b5c7-4add-b057-f44b1a5e8e91",
   :name "Test1",
   :exp 1645432091,
   :username "fantastic-sun-69l94",
   :mode "test",
   :createdAt "2022-02-14T08:28:11.620Z",
   :userUuid "45355491-519b-4839-bd69-e0064e2d259b",
   :isConfirmed false,
   :userId 5,
   :image
   "https://res.cloudinary.com/component/image/upload/avatars/avatar-plain-8.png",
   :iat 1644827291,
   :data {}}


  (let [access-token "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjI1M2ExMTliLWI1ZjctNGE1Ni04MTNiLWRlNDYwZDk2YTI4MiJ9.eyJtb2RlIjoidGVzdCIsInRlbmFudElkIjoiN244dzVycWIiLCJ1c2VySWQiOjUsInVzZXJVdWlkIjoiNDUzNTU0OTEtNTE5Yi00ODM5LWJkNjktZTAwNjRlMmQyNTliIiwiaXNDb25maXJtZWQiOmZhbHNlLCJhdXRob3JpemF0aW9uIjp7fSwiaXNzIjoidXNlcmZyb250Iiwic2Vzc2lvbklkIjoiYTgyOTVjODItYjVjNy00YWRkLWIwNTctZjQ0YjFhNWU4ZTkxIiwiaWF0IjoxNjQ0ODI3MjkxLCJleHAiOjE2NDU0MzIwOTF9.Hu7MnnHlEltNjUZ-vya4DOLvV51I8LrDyGUO6rwykkngSrPw_3YI-u6-BxxGPUgM6BPUV_xysY3e72wJsYvxoUKvdTDJCMQQ9JodF0UZ48N_eOKi6mufQPwyWYxJXu_W0myiMDR7lRPS7mR4LyNqQJ-5p1MQk5uuaW1DCzwKP01IuHxbb8fO0fMj2xa4aK44BJkID09z_fC524a4dAsVE1bJyevRiDldIf9SBc23Edi_ds_IiZl6GNMG0Me3kM0GI-BjJpVsKy7-JrJruhn7mmDOI_XOGw-b9Euquo1B0Z9oFPsV5TYtTyT1zhBgbwbyvketGy8HrfBthqLTRDovMIirGaVzY88XjamZffxSudKjyDesalVkDIvUhjbi6PXMlCzfkzvEfrCDIUGtxxRVUP6Po63UTxNmmVblOeZNPSlRTWiexjLDd49UH29gMnxrtII64bZf3lwAsgHKiWQsT49yJwHTa-Pl3IFwDSvLNZaZdQ1anIq_RKyU1eR_kGku1r8vYWBjZb3_awyxZik25F5rEVUgsGJ5oZbxe5wPyF-xmHRMkNJV1XDvos8wvrahRv0TCmI_sKn4vZONen3dTM0Z4zEV-uD4d0hfRPOUrUh8e1GZnzD8STXG99UHQRsSAStmvrtm_S-ZBoCsbQP2oJcQfUljFhLm5UDsrcqnzhA"
        id-token     "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjI1M2ExMTliLWI1ZjctNGE1Ni04MTNiLWRlNDYwZDk2YTI4MiJ9.eyJlbWFpbCI6ImphY3F1ZXNkcHpAem9oby5jb20iLCJ1c2VybmFtZSI6ImZhbnRhc3RpYy1zdW4tNjlsOTQiLCJuYW1lIjoiVGVzdDEiLCJpbWFnZSI6Imh0dHBzOi8vcmVzLmNsb3VkaW5hcnkuY29tL2NvbXBvbmVudC9pbWFnZS91cGxvYWQvYXZhdGFycy9hdmF0YXItcGxhaW4tOC5wbmciLCJkYXRhIjp7fSwiY29uZmlybWVkQXQiOm51bGwsImNyZWF0ZWRBdCI6IjIwMjItMDItMTRUMDg6Mjg6MTEuNjIwWiIsInVwZGF0ZWRBdCI6IjIwMjItMDItMTRUMDg6Mjg6MTEuNjIwWiIsIm1vZGUiOiJ0ZXN0IiwidXNlcklkIjo1LCJ1c2VyVXVpZCI6IjQ1MzU1NDkxLTUxOWItNDgzOS1iZDY5LWUwMDY0ZTJkMjU5YiIsInRlbmFudElkIjoiN244dzVycWIiLCJzZXNzaW9uSWQiOiJhODI5NWM4Mi1iNWM3LTRhZGQtYjA1Ny1mNDRiMWE1ZThlOTEiLCJpc0NvbmZpcm1lZCI6ZmFsc2UsImlhdCI6MTY0NDgyNzI5MSwiZXhwIjoxNjQ1NDMyMDkxfQ.Yxr_DuKBKj7xaxb242vAyRKz_X1-toPk1kmFxAl1TaRnsr9KZIZoR4UG1LZzPHDCk_eKEZ8kOg_RCcuDTcA_-n4MsyGm0xM5FXKqSWlP_6croV3OqBRzAFwuHIjdZMUfDhj2ErBpEij4BUbjjJIjwQJ5MoRz-Od-uGAsmfTtoeTCEUAbJzq4YZ3CNnKZN8hIcHlZHS_ztWZtXoZBR0ECqA5nCrxfD54Q9LshS7XeoBSpz9GrJucQ3L2KyOhWYYXQWcOkN6fjATdqc5M0oE9oxyU-3IBGl52lZ7pFdYYxRZEPo2rrpqnbCrfUP8xbfaVHCtXkmwjbmSvUNwBUwZ7OM2YYNY6fdWSXXW5lSVeScnKTu8NBBkAqnB2wsFyV9UJsGOP4aLZi1bPaMMXv_OLv1TeGjhaRyZeeqb650Hfw433J0mjSSdAZmTIheI56Xvm0xPNn2rbL9ct45jVpDcktCXWYa1v_jnO0Mn0xR_eH2SoRMelGkVKAwBO2ITBUwhsL2DS3e49YbjTVVvKpwZmIxLvyq6haqovkAFoX2o3v8DdbAr-N43YSuVySfluDRVvj0P3VMPRmbBj1GI73PB1gAg6Cr92EztK-h2W4mqpNFrH3-v_rGBXoQgKhzfVl9085h1nHxiWrZoSZ39R93fSqoPp4a02gV2w8Ocp4G3vgoA8"
        ref-token    "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjI1M2ExMTliLWI1ZjctNGE1Ni04MTNiLWRlNDYwZDk2YTI4MiJ9.eyJ1c2VySWQiOjUsInVzZXJVdWlkIjoiNDE5ZTIzZmUtNjk3NS00NzIzLTkxOTctNzVmZWM1NTEwNzFlIiwidGVuYW50SWQiOiI3bjh3NXJxYiIsIm1vZGUiOiJ0ZXN0IiwidHlwZSI6InJlZnJlc2giLCJzZXNzaW9uSWQiOiJhNTgyOGY0OC05ZWIxLTRjZTEtOGY2YS0xZDhlZDhiYWU3MDIiLCJpYXQiOjE2NDQyMjA5NDUsImV4cCI6MTY0NjgxMjk0NX0.abusFz9ApFiGzRrLXUvFNBbMTnhPbtdOyhPRvM2h05zYZPdbL5F44477YeZrB8Gb5BpMqjepfMvaHo1TzyVeG10mqcEUvO_lGDgCuRthRGBtk5cGA_DTcGtp07cUjfbNjphlPB3HTmFZJ83jKopz1KI3mIhrPqdUH1npWy4dcmsACoki5yw1VflaJ6P8srLi3OCmF-Clh3yWN7-dgmachyse2UUvnLq6dooDfdPDtIohtE4yEUG2-tCoOZzGhwwp3tasRSBzFy1tSbH2zPPvAMXq8_qohSQC0ZSoNLk7BtIKvX3UrCbjr3zmDTNRSJJRg5RCAYgnoZb9Mm-G6UBJcMXfsIv_hj7rDjGNxGiOVBkiGApKVsjLoNMzjtjwaHZFFZsxr5_nsBtMiZpp-GvNnDLk1i7w0zZ00t-G-pFv9UaJQi_aiZsgpzDs7JEyPTkvXOwfaLjPosZTUbBLF-eePpLM5OtpBUCAqnbP3ZJQIt7fumfdXBPElZVwWBdG-d4sCp-l-HdLn0Vw0_JXoecUj0G19pypIw33PZC0C3cYZc0yGa5ZolztXa5_iUkhLpImZ3iCbENpPdE9RsVwOjjurLoNtGur2SaCkZsHvmdN3Kml2ZieSRp-PNvd39_3iZSV490TWRO6Azb2zgmbRd3a1iplt29Jgb46S55oE9-NNMA"]
    (verify-jwt-token id-token))


  )
