(ns remote-log.util
  (:require [ring.util.response :as response]
            [buddy.auth.backends.token :as token]
            [buddy.auth.protocols :as proto]
            [clojure.set :as cset])
  (:import (java.util UUID)))

(defn bad-request [s]
  (-> (response/response s)
      (response/content-type "text/plain")
      (response/status 400)))

(defn parse-long [s]
  (when (seq s)
    (Long/parseLong s)))

(defn convert
  "ks can be either a map {:a :new-a} or a collection [:a]"
  ([m ks f] (convert m ks f m))
  ([m ks f init]
   (let [km (if (-> ks first coll?) ks (map #(list % %) ks))]
     (reduce (fn [r [k1 k2]] (assoc r k2 (f (k1 m)))) init km))))

(defn jws-backend [options param]
  (let [that (token/jws-backend options)]
    (reify
      proto/IAuthentication
      (-parse [_ request]
        (or (proto/-parse that request)
            (->> (get-in request [:params param] "")
                 (re-matches #"([^\?]*).*")                 ; Workaround: in ajax mode ? is appened after ?param=...
                 second)))

      (-authenticate [_ request data]
        (proto/-authenticate that request data))

      proto/IAuthorization
      (-handle-unauthorized [_ request metadata]
        (proto/-handle-unauthorized that request metadata)))))

(defn rename-keys-with [m f]
  (let [oldk (keys m)
        newk (map f oldk)
        kmap (apply hash-map (interleave oldk newk))]
    (cset/rename-keys m kmap)))

(defn uuid [] (UUID/randomUUID))
