(defproject remote-log "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.6.1"

  :dependencies [[org.clojure/clojure "1.9.0-alpha7"]
                 [org.clojure/clojurescript "1.9.76"]
                 [reagent "0.5.1"]
                 [compojure "1.5.0"]
                 [ring/ring-defaults "0.2.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [http-kit "2.1.19"]
                 [ring/ring-json "0.4.0"]
                 [cljs-ajax "0.5.5"]
                 [buddy/buddy-auth "1.1.0"]
                 [buddy/buddy-sign "1.1.0"]
                 [clj-time "0.12.0"]
                 [environ "1.0.3"]
                 [com.taoensso/sente "1.8.1"]
                 [clojurewerkz/cassaforte "2.0.2"]
                 [camel-snake-kebab "0.4.0"]]

  :plugins [[lein-figwheel "0.5.3-2"]
            [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [;; This next build is an compressed minified build for
               ;; production. You can build this with:
               ;; lein cljsbuild once min
               {:id           "min"
                :source-paths ["src/cljs"]
                :compiler     {:output-to     "resources/public/js/compiled/main.js"
                               :main          remote-log.client
                               :optimizations :advanced
                               :pretty-print  false}}
               {:id           "dev"
                :source-paths ["src/cljs"]

                ;; If no code is to be run, set :figwheel true for continued automagical reloading
                :figwheel     true

                :compiler     {:main                 remote-log.client
                               :asset-path           "js/compiled/out"
                               :output-to            "resources/public/js/compiled/main.js"
                               :output-dir           "resources/public/js/compiled/out"
                               :source-map-timestamp true}}]}

  :figwheel {;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/public/css"]             ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this
             ;; doesn't work for you just run your own server :) (see lien-ring)

             ;; :ring-handler hello_world.server/handler

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you are using emacsclient you can just use
             ;; :open-file-command "emacsclient"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"
             }


  ;; setting up nREPL for Figwheel and ClojureScript dev
  ;; Please see:
  ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl

  :profiles {:dev     {:dependencies [[figwheel-sidecar "0.5.3-2"]
                                      [com.cemerick/piggieback "0.2.1"]
                                      [org.clojure/tools.nrepl "0.2.10"]
                                      [org.clojure/tools.namespace "0.2.11"]
                                      [com.gearswithingears/shrubbery "0.3.1"]
                                      [org.clojure/data.json "0.2.6"]]
                       ;; need to add dev source path here to get user.clj loaded
                       :source-paths ["dev"]
                       ;; for CIDER
                       ;; :plugins [[cider/cider-nrepl "0.12.0"]]
                       :repl-options {; for nREPL dev you really need to limit output
                                      :init             (set! *print-length* 50)
                                      :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
                                      :init-ns          user}}
             :uberjar {:main       remote-log.server
                       :aot        :all
                       :prep-tasks ["compile" ["cljsbuild" "once"]]}})
