(ns app.system.core
  (:require
    [app.system.impl :as impl]
    [lib.clojure.ns :as ns]
    [lib.integrant.core :as ig]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns/require-dir 'app.system.service._)
(ns/require-dir 'app.system.task._)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defonce ^{:doc "Global reference to the running system"
           :private true}
         var'system (atom nil))


(add-watch var'system :log-system-status
           (fn [_ _ _ system]
             (some-> system impl/log-prop-files)
             (some-> system impl/log-running-webapps)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- system-config
  "App system configuration."
  []
  {:app.system/dev-mode? false

   :app.system.service/ref'mount
   {:app.config.core/app-config (ig/ref :app.system.service/app-config)
    :app.database.core/ref'data-source-read-write (ig/ref :app.system.service/ref'hikari-data-source-read-write)
    :app.database.core/ref'data-source-read-only (ig/ref :app.system.service/ref'hikari-data-source-read-only)}


   [::impl/system-property :app.system.service/app-config.prop-files]
   {:key "config.file"}

   :dev.env.system/prepare-prop-files nil

   :app.system.service/app-config
   {:prop-files (ig/ref :app.system.service/app-config.prop-files)
    :dev/prepare-prop-files (ig/ref :dev.env.system/prepare-prop-files)
    :conform-rules {#"System\.Switch\..+" :edn
                    #".+\.Password" :secret
                    #".+\.Secret" :secret
                    #"Webapp\.Hosts\(.+\)" :set
                    "Development.DatabaseMigration" :edn}
    :prop-defaults {"HttpServer.Port" 8080}}


   ; Database

   [::impl/import-map :app.system.config/hikari-data-source]
   {:init-map {:dev-mode? (ig/ref :app.system/dev-mode?)}
    :import-from (ig/ref :app.system.service/app-config)
    :import-keys {:data-source-class "Database.DataSourceClassName"
                  :database-url "Database.Url"
                  :database-user "Database.User"
                  :database-password "Database.Password"}}

   :app.system.service/ref'hikari-data-source-read-write
   (ig/ref :app.system.config/hikari-data-source)

   :app.system.service/ref'hikari-data-source-read-only
   (ig/ref :app.system.config/hikari-data-source)


   [::impl/import-map :app.system.config/database-migration]
   {:init-map {:ref'data-source (ig/ref :app.system.service/ref'hikari-data-source-read-write)
               :changelog-path "app/database/migration/changelog.xml"
               :enabled? true}
    :import-from (ig/ref :app.system.service/app-config)
    :import-keys {:enabled? "Development.DatabaseMigration"}}


   :app.system.task/ref'database-migration
   (ig/ref :app.system.config/database-migration)


   ; Webapps

   [::impl/import-map :app.system.config/example-http-handler]
   {:init-map {:name "example"
               :dev-mode? (ig/ref :app.system/dev-mode?)}
    :import-from (ig/ref :app.system.service/app-config)
    :import-keys {:hosts "Webapp.Hosts(example)"}}

   [:app.system.service/webapp-http-handler :app.system.service/example-http-handler]
   (ig/ref :app.system.config/example-http-handler)

   :dev.env.system/prepare-webapp nil

   [::impl/import-map :app.system.config/http-server]
   {:init-map {:options {:host "0.0.0.0"}
               :webapps [(ig/ref :app.system.service/example-http-handler)]
               :dev/prepare-webapp (ig/ref :dev.env.system/prepare-webapp)
               :await-before-start (ig/ref :app.system.config/await-before-start)}
    :import-from (ig/ref :app.system.service/app-config)
    :import-keys {:options {:host "HttpServer.Host"
                            :port "HttpServer.Port"}}}

   :app.system.service/ref'immutant-web
   (ig/ref :app.system.config/http-server)


   ; Wrap up

   [::impl/import-map :app.system.config/await-before-start]
   {:init-map {:database-migration (ig/ref :app.system.task/ref'database-migration)
               :mount (ig/ref :app.system.service/ref'mount)}}})

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn stop!
  "Stop global system."
  []
  (when-some [system @var'system]
    (reset! var'system nil)
    (ig/halt! system)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn suspend!
  "Suspend global system."
  []
  (some-> @var'system (ig/suspend!)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn start!
  "Start global system."
  ([]
   (start! {}))
  ([{:keys [system-keys, prepare-config]
     :or {prepare-config identity}}]
   (stop!)
   (let [config (prepare-config (system-config))]
     (reset! var'system (ig/init config, (or system-keys (keys config)))))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn resume!
  "Resume global system."
  ([]
   (resume! {}))
  ([{:keys [system-keys, prepare-config]
     :or {prepare-config identity} :as options}]
   (if-some [system @var'system]
     (do
       (reset! var'system nil)
       (let [config (prepare-config (system-config))]
         (reset! var'system (ig/resume config, system, (or system-keys (keys system))))))
     (start! options))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(comment
  (time (keys (start!)))
  (time (suspend!))
  (time (keys (resume!)))
  (time (do
          (suspend!)
          (keys (resume!))))
  (time (stop!))

  (time (:app.system.service/ref'immutant-web (start! {:system-keys [:app.system.service/ref'immutant-web]}))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
