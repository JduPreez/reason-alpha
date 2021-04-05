(ns reason-alpha.domain
  (:require [clojure.spec.alpha :as s]
            [reason-alpha.utils :as utils]))

(s/def :trade-pattern/creation-id uuid?)

(s/def :trade-pattern/name utils/not-blank?)

(s/def :trade-pattern/id uuid?)

(s/def :trade-pattern/description utils/not-blank?)

(s/def :trade-pattern/parent-id uuid?)

(s/def :trade-pattern/user-id uuid?)

(s/def :trade-pattern/ancestors-path seq)

(s/def ::trade-pattern (s/keys :req [:trade-pattern/creation-id
                                     :trade-pattern/name
                                     :trade-pattern/ancestors-path]
                              :opt [:trade-pattern/id
                                    :trade-pattern/description
                                    :trade-pattern/parent-id
                                    :trade-pattern/user-id]))
