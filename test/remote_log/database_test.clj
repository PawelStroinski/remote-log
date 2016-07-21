(ns remote-log.database-test
  (:require [clojure.test :refer :all]
            [remote-log.database :refer :all]))

(deftest entry->rec'
  (is (= {:platform  "droid"
          :level     "info"
          :device_id "1"}
         (entry->rec {:platform  :droid
                      :level     :info
                      :device-id "1"}))))

(deftest rec->entry'
  (is (= {:platform  :droid
          :level     :info
          :device-id "1"
          :occurred  10000000
          :received  20000000}
         (rec->entry {:platform "droid"
                      :level     "info"
                      :device_id "1"
                      :occurred #inst "1970-01-01T02:46:40.000Z"
                      :received #inst "1970-01-01T05:33:20.000Z"}))))
