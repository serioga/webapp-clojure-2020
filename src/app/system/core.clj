(ns app.system.core
  (:require [app.system.config :as config]
            [app.system.integrant :as ig']
            [integrant.core :as ig]
            [lib.clojure-tools-logging.logger :as logger]
            [lib.clojure.core :as c]
            [lib.clojure.ns :as ns]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns/require-dir 'app.system.service._)
(ns/require-dir 'app.system.task._)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- config-template
  []
  {::config/dev-mode {:config false}

   :app.system.service/app-config {:as ::config/app-config
                                   :mounts [:app.config.core/app-config]
                                   :config {:conform-rules {#"System\.Switch\..+" :edn
                                                            #".+\.Password" :secret
                                                            #".+\.Secret" :secret
                                                            #"Webapp\.Hosts\(.+\)" :set
                                                            "Development.DatabaseMigration" :edn}
                                            :prop-defaults {"HttpServer.Port" 8080}}}

   ::data-source-read-write {:as ::config/hikari-data-source
                             :mounts [:app.database.core/data-source-read-write
                                      :app.database.hugsql/data-source-read-write]}

   ::data-source-read-only {:as ::config/hikari-data-source
                            :mounts [:app.database.core/data-source-read-only
                                     :app.database.hugsql/data-source-read-only]
                            :config {:read-only true}}

   :app.system.task/database-migration {:config {:data-source (ig/ref ::data-source-read-write)
                                                 :changelog-path "app/database/migration/changelog.xml"
                                                 :system-is-enabled true}
                                        :import {:system-is-enabled "Development.DatabaseMigration"}}

   :app.system.service/immutant-web {:as ::config/http-server
                                     :webapps {:app.system.service/homepage-http-handler {:config {:name "example"}
                                                                                          :import {:hosts "Webapp.Hosts(example)"}}}
                                     :config {:options {:host "0.0.0.0"}}
                                     :import {:options {:host "HttpServer.Host"
                                                        :port "HttpServer.Port"}}
                                     :awaits [:app.system.task/database-migration
                                              :app.system.service/mount]}})

(defn- system-config
  "Returns app system configuration."
  []
  (config/build-config (config-template)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defonce ^{:doc "Global reference to the running system"
           :private true}
  system! (atom nil))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn stop
  "Stop global system."
  []
  (when-some [system @system!]
    (reset! system! nil)
    (ig'/halt! system)
    (logger/info (logger/get-logger *ns*) "[DONE] Application system stop")))

(defn suspend
  "Suspend global system."
  []
  (some-> @system! (ig'/suspend!)))

(defn start
  "Start global system."
  {:arglists '([] [{:keys [:system-keys :prepare-config]}])}
  ([]
   (start {}))
  ([options]
   (stop)
   (let [config ((:prepare-config options identity) (system-config))]
     (reset! system! (ig'/init config, (or (:system-keys options) (keys config)))))
   (logger/info (logger/get-logger *ns*) "[DONE] Application system start")))

(defn resume
  "Resume global system."
  {:arglists '([] [{:keys [:system-keys :prepare-config]}])}
  ([]
   (resume {}))
  ([options]
   (if-some [system @system!]
     (do
       (reset! system! nil)
       (let [config ((:prepare-config options identity) (system-config))]
         (reset! system! (ig'/resume config, system, (or (:system-keys options) (keys system))))))
     (start options))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- log-running-webapps
  "Log info about running webapps (URLs with host/port)."
  [system]
  (let [webapps (some-> system :app.system.service/immutant-web meta :running-webapps)]
    (doseq [[webapp-name {:keys [host port ssl-port virtual-host]}] webapps
            webapp-host (cond (sequential? virtual-host) virtual-host
                              (string? virtual-host) [virtual-host]
                              :else [(or host "localhost")])]
      (logger/info (logger/get-logger *ns*)
                   (print-str "Running webapp" (pr-str webapp-name)
                              (str (when port (str "- http://" webapp-host ":" port "/")))
                              (str (when ssl-port (str "- https://" webapp-host ":" ssl-port "/"))))))))

(defn- log-prop-files
  "Log info about loaded configuration files."
  [system]
  (let [prop-files (some-> system :app.system.service/app-config meta :prop-files)]
    (logger/info (logger/get-logger *ns*) (c/pr-str* "Running config from" prop-files))))

(add-watch system! :log-system-status
           (fn [_ _ _ system]
             (some-> system (doto (log-prop-files)
                                  (log-running-webapps)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
