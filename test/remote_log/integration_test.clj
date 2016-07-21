(ns remote-log.integration-test
  (:require [clojure.test :refer :all]
            [org.httpkit.client :refer [request]]
            [clojure.data.json :as json]
            [user :as user]
            [remote-log.server-test :as server-test]))

(def system nil)

(defn fixture [f]
  (user/init #'system 3001)
  (user/start #'system)
  (f)
  (user/stop #'system))

(use-fixtures :once fixture)

(def base-url "http://localhost:3001/")

(defn json-post [url body]
  (request {:url     url
            :method  :post
            :body    (json/write-str body)
            :headers {"Content-Type" "application/json"}}))

(defn acquire-token []
  (let [resp @(json-post (str base-url "acquire-token")
                         {:user "admin"
                          :pass "admin"})]
    (-> resp :body json/read-str (get "token"))))

(defn secured-json-get [url query-params]
  (let [make-request #(request {:url          url
                                :query-params query-params
                                :headers      {"Authorization"
                                               (str "Token " %)}})
        token (acquire-token)
        using-invalid-token (make-request "foo")
        using-valid-token (make-request token)]
    (is (= [401 "Unauthorized"]
           ((juxt :status :body) @using-invalid-token)))
    (is (= 200
           (:status @using-valid-token)))
    (-> @using-valid-token :body json/read-str)))

(deftest get-root
  (let [resp @(request {:url base-url})]
    (is (clojure.string/includes?
          (:body resp)
          "js/compiled/main.js"))))

(deftest post-entry
  (let [resp @(json-post (str base-url "entry")
                         (-> server-test/req
                             :body
                             (assoc :app "App2")))]
    (is (= 201
           (:status resp)))))

(deftest get-entries
  (let [entries (secured-json-get (str base-url "entries")
                                  {:app "App1"})]
    (is (seq
          entries))
    (is (string?
          (-> entries first (get "level"))))))

(deftest get-apps
  (let [apps (secured-json-get (str base-url "apps") {})]
    (is (seq
          apps))
    (is (string?
          (first apps)))))

(deftest post-to-acquire-token
  (is (string?
        (acquire-token))))
