(ns remote-log.database
  (:require [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer :all]
            [com.stuartsierra.component :as component]
            [clj-time.coerce :as tc]
            [camel-snake-kebab.core :refer [->kebab-case ->snake_case]]
            [remote-log.util :as util]
            [remote-log.model :as model]))

(defrecord Db [session options initialize]
  component/Lifecycle
  (start [this]
    (let [session (cc/connect ["127.0.0.1"] (select-keys options [:port]))]
      (initialize session options)
      (assoc this :session session)))
  (stop [this]
    (cc/disconnect! session)
    (assoc this :session nil)))

(defn new-db [options initialize]
  (map->Db {:options    options
            :initialize initialize}))

(defn init-keyspace [session {:keys [keyspace]}]
  (cql/create-keyspace session keyspace
                       (if-not-exists)
                       (with {:replication
                              {:class              "SimpleStrategy"
                               :replication_factor 1}}))
  (cql/use-keyspace session keyspace))

(defn create-entry-table [session]
  (cql/create-table session :entry
                    (if-not-exists)
                    (column-definitions {:app          :varchar
                                         :platform     :varchar
                                         :version      :varchar
                                         :level        :varchar
                                         :message      :varchar
                                         :device_id    :varchar
                                         :device_model :varchar
                                         :os_version   :varchar
                                         :occurred     :timestamp
                                         :signature    :varchar
                                         :received     :timestamp
                                         :id           :uuid
                                         :remote_addr  :varchar
                                         :primary-key  [[:app] :received :id]})
                    (with {:clustering-order [[:received :desc]]})))

(defn init [session options]
  (init-keyspace session options)
  (create-entry-table session))

(defprotocol EntryDb
  (insert-entry [db entry])
  (get-entries [db app before after page-size])
  (get-apps [db]))

(defn entry->rec [entry]
  (-> (util/convert entry model/keyword-keys name)
      (util/rename-keys-with ->snake_case)))

(defn rec->entry [rec]
  (-> (util/rename-keys-with rec ->kebab-case)
      (util/convert model/keyword-keys keyword)
      (util/convert [:occurred :received] tc/to-long)))

(extend-type Db
  EntryDb
  (insert-entry [{:keys [session]} entry]
    (cql/insert session :entry (entry->rec entry)))
  (get-entries [{:keys [session]} app before after page-size]
    (let [criteria (remove nil? [[= :app app]
                                 (when before [< :received before])
                                 (when after [> :received after])])]
      (->> (cql/select session :entry
                       (where criteria)
                       (limit page-size))
           reverse
           (map rec->entry))))
  (get-apps [{:keys [session]}]
    (->> (cc/execute session "SELECT DISTINCT app FROM entry")
         (map :app))))
