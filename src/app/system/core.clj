(ns app.system.core
  (:require [clojure.tools.logging :as log]
            [lib.clojure.core :as e]
            [lib.clojure.ns :as ns]
            [lib.integrant.core :as ig]
            [lib.integrant.system :as system]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns/require-dir 'app.system.service._)
(ns/require-dir 'app.system.task._)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- add-to-await-before-start
  [system key]
  (let [key (ig/decompose-key key)]
    (update system [:lib.integrant.system/import-map :app.system.config/await-before-start]
            assoc-in [:init-map key] (ig/ref key))))

(defn- mount-refs
  "Creates references from vector of `mounts` keywords to `key` in `:app.system.service/mount`.
   If `key` is composite then only the last value is taken."
  [system key mounts]
  (let [key (ig/decompose-key key)]
    (reduce (fn [system mount-key]
              (-> system
                  (update :app.system.service/ref'mount assoc mount-key (ig/ref key))
                  (add-to-await-before-start :app.system.service/ref'mount)))
            system mounts)))

#_(defn- add-with-mount
    "Add simple configuration with `mounts` references."
    [system key config mounts]
    (-> (assoc system key config)
        (mount-refs key mounts)))

(defn- key-with-suffix
  "Adds 'extension' to keyword `key`."
  [key ext]
  (let [key (ig/decompose-key key)]
    (keyword (namespace key) (str (name key) ext))))

(defn- import-app-config
  "Creates pair of keys for service `key` and its configuration `config-key` imported from app-config.
   Also mounts references for optional vector of `mounts`."
  [system, key {:keys [config, import, mounts]}]
  (let [config-key (key-with-suffix key ".config")]
    (-> system
        (merge {[::system/import-map config-key] {:init-map config
                                                  :import-from (ig/ref :app.system.service/app-config)
                                                  :import-keys import}
                key (ig/ref config-key)})
        (mount-refs key mounts))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(derive :app.system/dev-mode? ::system/identity)
(derive :dev.env.system/prepare-prop-files ::system/identity)
(derive :dev.env.system/prepare-webapp ::system/identity)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- new-config
  []
  {:app.system/dev-mode? false})

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- app-config
  [system {:keys [config mounts]}]
  (-> system (merge {:dev.env.system/prepare-prop-files nil

                     [::system/system-property :app.system.service/app-config.prop-files]
                     {:key "config.file"}

                     :app.system.service/app-config
                     (merge {:prop-files (ig/ref :app.system.service/app-config.prop-files)
                             :dev/prepare-prop-files (ig/ref :dev.env.system/prepare-prop-files)}
                            config)})
      (mount-refs :app.system.service/app-config mounts)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- hikari-data-source
  [system key {:keys [mounts]}]
  (import-app-config system key
                     {:config {:dev-mode? (ig/ref :app.system/dev-mode?)}
                      :import {:data-source-class "Database.DataSourceClassName"
                               :database-url "Database.Url"
                               :database-user "Database.User"
                               :database-password "Database.Password"}
                      :mounts mounts}))

(defn- database-migration
  {:arglists '([system key {:keys [config, import, mounts]}])}
  [system key params]
  (-> system
      (import-app-config key params)
      (add-to-await-before-start key)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- http-server
  [system {:keys [webapps] :as params}]
  (-> (reduce (fn [system [key params]]
                (import-app-config system [:app.system.service/webapp-http-handler key]
                                   (update params :config assoc :dev-mode? (ig/ref :app.system/dev-mode?))))
              system webapps)
      (assoc :dev.env.system/prepare-webapp nil)
      (import-app-config :app.system.service/ref'immutant-web
                         (update params :config (partial e/deep-merge {:webapps (->> webapps (mapv (comp ig/ref first)))
                                                                       :dev/prepare-webapp (ig/ref :dev.env.system/prepare-webapp)
                                                                       :await-before-start (ig/ref :app.system.config/await-before-start)})))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- system-config
  []
  (-> (new-config)

      (app-config {:mounts [:app.config.core/app-config]
                   :config {:conform-rules {#"System\.Switch\..+" :edn
                                            #".+\.Password" :secret
                                            #".+\.Secret" :secret
                                            #"Webapp\.Hosts\(.+\)" :set
                                            "Development.DatabaseMigration" :edn}
                            :prop-defaults {"HttpServer.Port" 8080}}})

      (hikari-data-source :app.system.service/ref'hikari-data-source-read-write
                          {:mounts [:app.database.core/ref'data-source-read-write]})

      (hikari-data-source :app.system.service/ref'hikari-data-source-read-only
                          {:mounts [:app.database.core/ref'data-source-read-only]})

      (database-migration :app.system.task/ref'database-migration
                          {:config {:ref'data-source (ig/ref :app.system.service/ref'hikari-data-source-read-write)
                                    :changelog-path "app/database/migration/changelog.xml"
                                    :enabled? true}
                           :import {:enabled? "Development.DatabaseMigration"}})

      (http-server {:webapps {:app.system.service/homepage-http-handler {:config {:name "example"}
                                                                         :import {:hosts "Webapp.Hosts(example)"}}}
                    :config {:options {:host "0.0.0.0"}}
                    :import {:options {:host "HttpServer.Host"
                                       :port "HttpServer.Port"}}})))

(comment
  (->> (system-config) keys)
  (->> (system-config) keys (map ig/decompose-key) sort)
  (-> (system-config) (get :app.system.service/ref'mount) keys sort)
  (-> (system-config) (get [::system/import-map :app.system.config/await-before-start])))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defonce ^{:doc "Global reference to the running system"
           :private true}
         var'system (atom nil))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn stop!
  "Stop global system."
  []
  (when-some [system @var'system]
    (reset! var'system nil)
    (ig/halt! system)))

(defn suspend!
  "Suspend global system."
  []
  (some-> @var'system (ig/suspend!)))

(defn start!
  "Start global system."
  ([]
   (start! {}))
  ([{:keys [system-keys, prepare-config]
     :or {prepare-config identity}}]
   (stop!)
   (let [config (prepare-config (system-config))]
     (reset! var'system (ig/init config, (or system-keys (keys config)))))))

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

(defn- log-running-webapps
  "Log info about running webapps (URLs with host/port)."
  [system]
  (let [webapps (some-> system
                        :app.system.service/ref'immutant-web
                        (deref)
                        (meta)
                        :running-webapps)]
    (doseq [[name {:keys [host port ssl-port virtual-host]}] webapps
            webapp-host (cond (sequential? virtual-host) virtual-host
                              (string? virtual-host) [virtual-host]
                              :else [(or host "localhost")])]
      (log/info "Running" "webapp" (pr-str name)
                (str (when port (str "- http://" webapp-host ":" port "/")))
                (str (when ssl-port (str "- https://" webapp-host ":" ssl-port "/")))))))

(defn- log-prop-files
  "Log info about loaded configuration files."
  [system]
  (let [prop-files (some-> system
                           :app.system.service/app-config
                           (meta)
                           :prop-files)]
    (log/info "Running config from" (pr-str prop-files))))

(add-watch var'system :log-system-status
           (fn [_ _ _ system]
             (some-> system log-prop-files)
             (some-> system log-running-webapps)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
