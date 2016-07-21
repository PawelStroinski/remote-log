(ns user
  (:require
    [figwheel-sidecar.repl-api :as f]
    [com.stuartsierra.component :as component]
    [clojure.tools.namespace.repl :refer (refresh)]
    [remote-log.server :as server]
    [remote-log.database :as database]
    [remote-log.util :as util]
    [clojure.pprint :refer (pprint)]
    [clj-time.coerce :as tc]))

(defn fig-start
  "This starts the figwheel server and watch based auto-compiler."
  []
  ;; this call will only work are long as your :cljsbuild and
  ;; :figwheel configurations are at the top level of your project.clj
  ;; and are not spread across different lein profiles

  ;; otherwise you can pass a configuration into start-figwheel! manually
  (f/start-figwheel!))

(defn fig-stop
  "Stop the figwheel server and watch based auto-compiler."
  []
  (f/stop-figwheel!))

;; if you are in an nREPL environment you will need to make sure you
;; have setup piggieback for this to work
(defn repl
  "Launch a ClojureScript REPL that is connected to your build and host environment."
  []
  (f/cljs-repl))

(defrecord DevDb [seed]
  component/Lifecycle
  (start [this]
    (assoc this :atom (atom seed)))
  (stop [this]
    (dissoc this :atom)))

(extend-type DevDb
  database/EntryDb
  (insert-entry [db entry]
    (swap! (:atom db) #(update % :entries (fnil conj []) entry)))
  (get-entries [db app before after page-size]
    (let [before (or before Long/MAX_VALUE)
          after (or after Long/MIN_VALUE)]
      (->> db :atom deref :entries
           (filter (comp #{app} :app))
           (sort-by :received)
           reverse
           (filter (fn [{:keys [received]}] (< received before)))
           (filter (fn [{:keys [received]}] (> received after)))
           (take page-size)
           reverse)))
  (get-apps [db]
    (->> db :atom deref :entries (map :app) distinct)))

(def system nil)

(defn dev-db []
  @(-> system :db :atom))

(def seed {:entries [{:app          "App1",
                      :platform     :droid,
                      :version      "1",
                      :level        :info,
                      :message      "System.ArgumentNullException: [ArgumentNull_Generic]\nArguments: \nDebugging resource strings are unavailable. Often the key and arguments provide sufficient information to diagnose the problem. See http://go.microsoft.com/fwlink/?linkid=106663&Version=4.0.30508.0&File=mscorlib.dll&Key=ArgumentNull_Generic\nParameter name: source\n   at System.Linq.Enumerable.Where[TSource](IEnumerable`1 source, Func`2 predicate)\n   at Dietphone.ViewModels.ProductListingViewModel.FindProduct(Guid productId)\n   at Dietphone.Views.ProductListing.RestoreTopItem()\n   at Dietphone.Views.ProductListing.Untombstone()\n   at Dietphone.Views.ProductListing.ViewModel_Loaded(Object sender, EventArgs e)\n   at System.EventHandler.Invoke(Object sender, EventArgs e)\n   at Dietphone.ViewModels.SubViewModel.OnLoaded()\n   at Dietphone.ViewModels.ProductListingViewModel.<Load>b__0(Object , EventArgs )\n   at Dietphone.ViewModels.LoaderBase.OnLoaded()\n   at Dietphone.ViewModels.LoaderBase.WorkCompleted()\n   at Dietphone.ViewModels.ProductListingViewModel.CategoriesAndProductsLoader.WorkCompleted()\n   at Dietphone.ViewModels.LoaderBase.<LoadAsync>b__1(Object , RunWorkerCompletedEventArgs )\n   at System.ComponentModel.BackgroundWorker.OnRunWorkerCompleted(RunWorkerCompletedEventArgs e)\n   at System.ComponentModel.BackgroundWorker.<OnRun>b__1(Object state)",
                      :device-id    "fon001",
                      :device-model "fon",
                      :os-version   "1.2.3"
                      :occurred     (tc/to-long #inst"2016-06-11T12:09:50.340-00:00"),
                      :signature    "61849448bdbb6761849448bdbb6761849448bdbb67",
                      :received     (tc/to-long #inst"2016-06-11T12:09:51.340-00:00")
                      :id           (util/uuid)
                      :remote-addr  "0:0:0:0:0:0:0:1"}
                     {:app          "App1",
                      :platform     :droid,
                      :version      "1",
                      :level        :debug,
                      :message      "System.ArgumentNullException: [ArgumentNull_Generic]\nArguments: \nDebugging resource strings are unavailable. Often the key and arguments provide sufficient information to diagnose the problem. See http://go.microsoft.com/fwlink/?linkid=106663&Version=4.0.30508.0&File=mscorlib.dll&Key=ArgumentNull_Generic\nParameter name: source\n   at System.Linq.Enumerable.Where[TSource](IEnumerable`1 source, Func`2 predicate)\n   at Dietphone.ViewModels.ProductListingViewModel.FindProduct(Guid productId)\n   at Dietphone.Views.ProductListing.RestoreTopItem()\n   at Dietphone.Views.ProductListing.Untombstone()\n   at Dietphone.Views.ProductListing.ViewModel_Loaded(Object sender, EventArgs e)\n   at System.EventHandler.Invoke(Object sender, EventArgs e)\n   at Dietphone.ViewModels.SubViewModel.OnLoaded()\n   at Dietphone.ViewModels.ProductListingViewModel.<Load>b__0(Object , EventArgs )\n   at Dietphone.ViewModels.LoaderBase.OnLoaded()\n   at Dietphone.ViewModels.LoaderBase.WorkCompleted()\n   at Dietphone.ViewModels.ProductListingViewModel.CategoriesAndProductsLoader.WorkCompleted()\n   at Dietphone.ViewModels.LoaderBase.<LoadAsync>b__1(Object , RunWorkerCompletedEventArgs )\n   at System.ComponentModel.BackgroundWorker.OnRunWorkerCompleted(RunWorkerCompletedEventArgs e)\n   at System.ComponentModel.BackgroundWorker.<OnRun>b__1(Object state)",
                      :device-id    "fon001",
                      :device-model "fon",
                      :os-version   "1.2.3"
                      :occurred     (tc/to-long #inst"2016-06-11T12:08:21.454-00:00"),
                      :signature    "61849448bdbb6761849448bdbb6761849448bdbb67",
                      :received     (tc/to-long #inst"2016-06-11T12:08:22.454-00:00")
                      :id           (util/uuid)
                      :remote-addr  "0:0:0:0:0:0:0:1"}
                     {:app          "App2",
                      :platform     :droid,
                      :version      "1",
                      :level        :error,
                      :message      "System.ArgumentNullException: [ArgumentNull_Generic]\nArguments: \nDebugging resource strings are unavailable. Often the key and arguments provide sufficient information to diagnose the problem. See http://go.microsoft.com/fwlink/?linkid=106663&Version=4.0.30508.0&File=mscorlib.dll&Key=ArgumentNull_Generic\nParameter name: source\n   at System.Linq.Enumerable.Where[TSource](IEnumerable`1 source, Func`2 predicate)\n   at Dietphone.ViewModels.ProductListingViewModel.FindProduct(Guid productId)\n   at Dietphone.Views.ProductListing.RestoreTopItem()\n   at Dietphone.Views.ProductListing.Untombstone()\n   at Dietphone.Views.ProductListing.ViewModel_Loaded(Object sender, EventArgs e)\n   at System.EventHandler.Invoke(Object sender, EventArgs e)\n   at Dietphone.ViewModels.SubViewModel.OnLoaded()\n   at Dietphone.ViewModels.ProductListingViewModel.<Load>b__0(Object , EventArgs )\n   at Dietphone.ViewModels.LoaderBase.OnLoaded()\n   at Dietphone.ViewModels.LoaderBase.WorkCompleted()\n   at Dietphone.ViewModels.ProductListingViewModel.CategoriesAndProductsLoader.WorkCompleted()\n   at Dietphone.ViewModels.LoaderBase.<LoadAsync>b__1(Object , RunWorkerCompletedEventArgs )\n   at System.ComponentModel.BackgroundWorker.OnRunWorkerCompleted(RunWorkerCompletedEventArgs e)\n   at System.ComponentModel.BackgroundWorker.<OnRun>b__1(Object state)",
                      :device-id    "fon001",
                      :device-model "fon",
                      :os-version   "1.2.3"
                      :occurred     (tc/to-long #inst"2016-06-11T12:07:09.741-00:00"),
                      :signature    "61849448bdbb6761849448bdbb6761849448bdbb67",
                      :received     (tc/to-long #inst"2016-06-11T12:07:10.741-00:00")
                      :id           (util/uuid)
                      :remote-addr  "0:0:0:0:0:0:0:1"}]})

(defn init [v server-port]
  (alter-var-root v (constantly (-> (server/system
                                      {:server-port      (str server-port)
                                       :database-port    "9042"
                                       :keyspace         "remote_log"
                                       :users            "{\"admin\" \"admin\"}"
                                       :token-minutes    "1"
                                       :token-secret     "shh!!!"
                                       :signature-secret ":-)"
                                       :page-size        "10"})
                                    (assoc :db (DevDb. seed))))))

(defn start [v]
  (alter-var-root v #(when % (component/start %))))

(defn stop [v]
  (alter-var-root v #(when % (component/stop %))))

(defn go []
  (init #'system 3000)
  (start #'system))

(defn reset []
  (stop #'system)
  (refresh :after 'user/go))
