(ns remote-log.model-test
  (:require [clojure.test :refer :all]
            [remote-log.model :refer :all]))

(deftest version?'
  (are [r v] (= r (version? v))
             true "1"
             true "1.2.3.4"
             false nil
             false "a"))

(deftest signature?'
  (are [r v] (= r (signature? v))
             true "1a"
             true "123F"
             false nil
             false "1"
             false "123H"))
