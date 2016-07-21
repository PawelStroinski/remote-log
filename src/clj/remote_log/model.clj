(ns remote-log.model
  (:require [clojure.spec :as s]))

(def platform? #{:ios :droid :wp})
(def version-regex #"^\d+(\.\d+){0,3}$")
(def version? (s/and string? #(re-matches version-regex %)))
(def level? #{:info :debug :error})
(def signature-regex #"^([A-Fa-f0-9]{2})+$")
(def signature? (s/and string? #(re-matches signature-regex %)))

(s/def ::app string?)
(s/def ::platform platform?)
(s/def ::version version?)
(s/def ::level level?)
(s/def ::message string?)
(s/def ::device-id string?)
(s/def ::device-model string?)
(s/def ::os-version string?)
(s/def ::occurred int?)
(s/def ::signature signature?)
(s/def ::received int?)
(s/def ::id uuid?)
(s/def ::remote-addr string?)

(s/def ::entry (s/keys :req-un [::app ::platform ::version ::level ::message ::device-id ::device-model
                                ::os-version ::occurred ::signature ::received ::id ::remote-addr]))

(def keyword-keys [:platform :level])
