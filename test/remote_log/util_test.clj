(ns remote-log.util-test
  (:require [clojure.test :refer :all]
            [remote-log.util :refer :all]
            [buddy.auth.protocols :as proto]))

(deftest jws-backend'
  (let [options {:secret "shh!!!" :options {:alg :hs512}}
        sut (jws-backend options :foo)
        req {:params {:foo "bar?b"}}]
    (is (= "bar" (proto/-parse sut req)))))
