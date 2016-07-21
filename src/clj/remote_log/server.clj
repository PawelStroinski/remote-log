(ns remote-log.server
  (:gen-class)
  (:require [clojure.spec :as s]
            [compojure.core :as compojure :refer [GET POST]]
            [compojure.route :as route]
            [ring.middleware.defaults :as defaults]
            [ring.util.response :as response]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [com.stuartsierra.component :as component]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.sign.jwt :as jwt]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.core.mac :as mac]
            [buddy.core.codecs :as codecs]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :as sente-hk]
            [environ.core :refer [env]]
            [remote-log.model :as model]
            [remote-log.util :as util]
            [remote-log.database :as database]))

(defn req->entry [{:keys [body remote-addr]}]
  (-> (util/convert body model/keyword-keys keyword)
      (assoc :received (tc/to-long (time/now)))
      (assoc :id (util/uuid))
      (assoc :remote-addr remote-addr)))

(defn entry-signature-valid? [{:keys [message occurred signature]} {:keys [signature-secret]}]
  (let [input (str message ":" occurred)
        signature (codecs/hex->bytes signature)]
    (mac/verify input signature {:key signature-secret :alg :hmac+sha256})))

(defn post-entry-do [db entry chsk-send!]
  (database/insert-entry db entry)
  (chsk-send! :sente/all-users-without-uid [:remote-log/push-app (:app entry)])
  (response/created ""))

(defn post-entry [db req cfg chsk-send!]
  (let [entry (req->entry req)]
    (cond
      (not (s/valid? ::model/entry entry)) (util/bad-request (s/explain-str ::model/entry entry))
      (not (entry-signature-valid? entry cfg)) (util/bad-request "Wrong signature")
      :else (post-entry-do db entry chsk-send!))))

(defn get-entries [db app before after {:keys [page-size]}]
  (when (seq app)
    (->> (database/get-entries db app (util/parse-long before) (util/parse-long after) page-size)
         (map #(util/convert % {:occurred :occurred-at
                                :received :received-at} (comp tc/to-date tc/from-long)))
         (map #(dissoc % :app :occurred :signature :id)))))

(defn secured
  ([req f] (secured req f response/response))
  ([req f wrap]
   (if (authenticated? req)
     (wrap (f))
     (throw-unauthorized))))

(defn acquire-token [user pass {:keys [users token-minutes token-secret]}]
  (if (some-> (users user) (= pass))
    (let [claims {:exp (time/plus (time/now) (time/minutes token-minutes))}]
      (->> (jwt/sign claims token-secret {:alg :hs512})
           (hash-map :token) response/response))
    (util/bad-request "Wrong user/pass")))

(defn routes [db cfg]
  (let [{:keys [send-fn ajax-get-or-ws-handshake-fn ajax-post-fn]}
        (sente/make-channel-socket! (sente-hk/->HttpKitServerChanAdapter) {})]
    (compojure/routes
      (GET "/" []
        (-> (response/resource-response "public/index.html")
            (response/content-type "text/html")))
      (POST "/entry" req
        (post-entry db req cfg send-fn))
      (GET "/entries" [app before after :as req]
        (secured req #(get-entries db app before after cfg)))
      (GET "/apps" req
        (secured req #(database/get-apps db)))
      (POST "/acquire-token" {{:keys [user pass]} :body}
        (acquire-token user pass cfg))
      (GET "/chsk" req
        (secured req #(ajax-get-or-ws-handshake-fn req) identity))
      (POST "/chsk" req
        (secured req #(ajax-post-fn req) identity))
      (route/resources "/"))))

(defn handler [db {:keys [token-secret] :as cfg}]
  (let [auth-backend (util/jws-backend {:secret token-secret :options {:alg :hs512}} :token)]
    (-> (routes db cfg)
        (wrap-authorization auth-backend)
        (wrap-authentication auth-backend)
        wrap-json-response
        (wrap-json-body {:keywords? true})
        (defaults/wrap-defaults defaults/api-defaults))))

(defrecord Server [options make-handler db stop-server]
  component/Lifecycle
  (start [this]
    (let [handler (make-handler db)]
      (assoc this :stop-server (run-server handler options))))
  (stop [this]
    (stop-server)
    (assoc this :stop-server nil)))

(defn server [options make-handler]
  (map->Server {:options      options
                :make-handler make-handler}))

(defn system [config-options]
  (let [convert (fn [ks] (util/convert config-options ks read-string {}))
        database-options (merge (convert {:database-port :port})
                                (select-keys config-options [:keyspace]))
        server-options (convert {:server-port :port})
        app-cfg (merge (convert [:users :token-minutes :page-size])
                       (select-keys config-options [:token-secret :signature-secret]))
        make-handler (fn [db] (handler db app-cfg))]
    (-> (component/system-map
          :db (database/new-db database-options database/init)
          :server (server server-options make-handler))
        (component/system-using
          {:server [:db]}))))

(defn -main [] (-> env system component/start))
