(ns remote-log.server-test
  (:require [clojure.test :refer :all]
            [shrubbery.core :refer :all]
            [remote-log.server :refer :all]
            [remote-log.database :as database]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [clojure.string :refer [upper-case]])
  (:import (java.util UUID)))

(def req {:body        {:app          "App1",
                        :platform     "droid",
                        :version      "1",
                        :level        "info",
                        :message      "Oops",
                        :device-id    "fon001",
                        :device-model "fon",
                        :os-version   "1.2.3"
                        :occurred     1466033195671,
                        :signature    "859167D2B7A181CD152C2E5FF674A608C2024D4CDEC09B9F4C27E7C4380C3955"}
          :remote-addr "0:0:0:0:0:0:0:1"})

(def entry (-> (req->entry req) (assoc :received 1466033200000)))

(deftest req->entry'
  (let [entry (req->entry req)]
    (is (= :droid (:platform entry)))
    (is (= :info (:level entry)))
    (is (int? (:received entry)))
    (is (instance? UUID (:id entry)))
    (is (= "0:0:0:0:0:0:0:1" (:remote-addr entry)))))

(deftest entry-signature-valid?'
  (is (entry-signature-valid? entry {:signature-secret ":-)"}))
  (is (not (entry-signature-valid? entry {:signature-secret ":-("}))))

(deftest post-entry-do'
  (let [db (spy (reify database/EntryDb (database/insert-entry [_ _] nil)))
        chsk-send!-args (atom nil)
        chsk-send! #(reset! chsk-send!-args %&)]
    (is (= 201 (:status (post-entry-do db entry chsk-send!))))
    (is (received? db database/insert-entry [entry]))
    (is (= [:sente/all-users-without-uid [:remote-log/push-app "App1"]] @chsk-send!-args))))

(deftest post-entry'
  (let [post-entry-do-args (atom nil)
        db (fn [])
        cfg {:signature-secret ":-)"}
        chsk-send! (fn [& _])]
    (with-redefs [post-entry-do (fn [& args] (reset! post-entry-do-args args) ::ret)
                  req->entry (fn [req'] (when (= req' req) entry))]
      (testing "version missing"
        (let [req (assoc-in req [:body :version] nil)]
          (is (= 400 (:status (post-entry db req cfg chsk-send!))))
          (is (nil? @post-entry-do-args))))
      (testing "wrong signature"
        (let [cfg (assoc cfg :signature-secret ":-(")]
          (is (= 400 (:status (post-entry db req cfg chsk-send!))))
          (is (nil? @post-entry-do-args))))
      (testing "ok"
        (is (= ::ret (post-entry db req cfg chsk-send!)))
        (is (= [db entry chsk-send!] @post-entry-do-args))))))

(deftest get-entries'
  (let [expected (repeat 3 {:platform     :droid,
                            :version      "1",
                            :level        :info,
                            :message      "Oops",
                            :device-id    "fon001",
                            :device-model "fon",
                            :os-version   "1.2.3"
                            :received     1466033200000
                            :remote-addr "0:0:0:0:0:0:0:1"
                            :occurred-at  #inst"2016-06-15T23:26:35.671-00:00",
                            :received-at  #inst"2016-06-15T23:26:40.000-00:00"})
        db (reify
             database/EntryDb
             (database/get-entries [_ app before after page-size]
               (when (and (= "foo" app) (= 1 before) (= 2 after) (= 3 page-size))
                 (repeat 3 entry))))
        cfg {:page-size 3}]
    (testing "app missing"
      (is (nil? (get-entries db nil "1" "2" cfg)))
      (is (nil? (get-entries db "" "1" "2" cfg))))
    (testing "ok"
      (is (= expected (get-entries db "foo" "1" "2" cfg))))))

(deftest secured'
  (let [f #(-> "foo")]
    (testing "not authenticated"
      (with-redefs [authenticated? (fn [r] (not= req r))
                    throw-unauthorized #(-> ::throwed)]
        (is (= ::throwed (secured req f)))
        (is (= ::throwed (secured req f upper-case)))))
    (testing "ok"
      (with-redefs [authenticated? (fn [r] (= req r))]
        (is (= "foo" (:body (secured req f))))
        (is (= "FOO" (secured req f upper-case)))))))

(deftest acquire-token'
  (let [cfg {:users {"foo" "bar"}
             :token-minutes 1
             :token-secret "shh!!!"}]
    (testing "wrong pass"
      (is (= 400 (:status (acquire-token "foo" "baz" cfg))))
      (is (= 400 (:status (acquire-token "bar" "baz" cfg)))))
    (testing "ok"
      (is (string? (get-in (acquire-token "foo" "bar" cfg) [:body :token]))))))
