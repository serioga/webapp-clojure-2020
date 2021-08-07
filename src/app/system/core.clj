(ns app.system.core
  (:require [clojure.tools.logging :as log]
            [lib.clojure.core :as e]
            [lib.clojure.ns :as ns]
            [lib.integrant.core :as ig]
            [lib.integrant.system :as system]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns/require-dir 'app.system.service._)
(ns/require-dir 'app.system.task._)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(derive ::dev-mode ::system/identity)
(derive ::await-before-start ::system/identity)
(derive :dev.env.system/prepare-prop-files ::system/identity)
(derive :dev.env.system/prepare-webapp ::system/identity)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- key-with-suffix
  "Adds 'extension' to keyword `key`."
  [key ext]
  (let [key (ig/decompose-key key)]
    (keyword (namespace key) (str (name key) ext))))

(defn- add-to-await-before-start
  [system key]
  (let [key (ig/decompose-key key)]
    (update system ::await-before-start assoc key (ig/ref key))))

(defn- mount-refs
  "Creates references from vector of `mounts` keywords to `key` in `:app.system.service/mount`.
   If `key` is composite then only the last value is taken."
  [system key mounts]
  (let [key (ig/decompose-key key)]
    (reduce (fn [system mount-key]
              (-> system
                  (update :app.system.service/mount assoc mount-key (ig/ref key))
                  (add-to-await-before-start :app.system.service/mount)))
            system mounts)))

(defmulti ^:private mixin
  "Resolves mixin represented by keyword.
   By default returns value as is."
  identity)

(e/add-method mixin :default identity)

(e/add-method mixin ::await-before-start
              (constantly {:config {:await-before-start (ig/ref ::await-before-start)}}))

(e/add-method mixin ::add-to-await-before-start
              (constantly {::add-to-await-before-start true}))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmulti ^:private install
  "Adds keys to the `system` for `key` with `params`."
  {:arglists '([system, key, {:keys [as, derive, config, import, mounts, mixins] :as params}])}
  (fn [_ _ params] (:as params)))

(defn- install*
  "Applies default `install` without specific :as."
  [system, key, params]
  (install system, key, (dissoc params :as)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- merge-mixins
  [{:keys [mixins] :as params}]
  (if mixins (apply e/deep-merge (dissoc params :mixins)
                    (map #(-> (mixin %) (e/assert map? "Mixin should be a map"))
                         (-> mixins (e/assert sequential? "Require :mixins to be a sequence"))))
             params))

(defn- import-app-config
  "Creates pair of keys for service `key` and its configuration `config-key` imported from app-config.
   Also mounts references for optional vector of `mounts`."
  [system, key {:keys [config, import]}]
  (let [config-key (cond-> key (ig/get-init-key key) (key-with-suffix ".config"))]
    (cond-> (assoc system [::system/import-map config-key] {:init-map (or config {})
                                                            :import-from (ig/ref :app.system.service/app-config)
                                                            :import-keys import})
      (not= key config-key) (assoc key (ig/ref config-key)))))

(defmethod install nil
  [system, key, params]
  (let [{:keys [derive, config, import, mounts] :as params} (merge-mixins params)
        key (cond->> key derive (vector derive))]
    (cond-> (if import (import-app-config system key params)
                       (assoc system key config))
      mounts,,,,,,,,,,,,,,,,,,,,,,,,,,,,,, (mount-refs key mounts)
      (::add-to-await-before-start params) (add-to-await-before-start key))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod install ::app-config
  [system key {:keys [config mounts]}]
  (let [prop-files (key-with-suffix key ".prop-files")]
    (-> system (merge {:dev.env.system/prepare-prop-files nil

                       [::system/system-property prop-files] {:key "config.file"}

                       key (merge {:prop-files (ig/ref prop-files)
                                   :dev/prepare-prop-files (ig/ref :dev.env.system/prepare-prop-files)}
                                  config)})
        (mount-refs key mounts))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod install ::hikari-data-source
  [system, key, params]
  (install* system key (e/deep-merge params {:derive :app.system.service/hikari-data-source
                                             :config {:dev-mode (ig/ref ::dev-mode)}
                                             :import {:data-source-class "Database.DataSourceClassName"
                                                      :database-url (if (-> params :config :read-only)
                                                                      "Database.Url.ReadOnly", "Database.Url")
                                                      :database-user "Database.User"
                                                      :database-password "Database.Password"}})))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod install ::http-server
  [system, key, {:keys [webapps] :as params}]
  (-> (reduce (fn [system [key params]]
                (install system [:app.system.service/webapp-http-handler key]
                         (update params :config merge {:dev-mode (ig/ref ::dev-mode)})))
              system webapps)
      (assoc :dev.env.system/prepare-webapp nil)
      (install* key (update params :config merge {:webapps (->> webapps (mapv (comp ig/ref first)))
                                                  :dev/prepare-webapp (ig/ref :dev.env.system/prepare-webapp)}))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- system-keys
  []
  {::dev-mode {:config false}

   :app.system.service/app-config {:as ::app-config
                                   :mounts [:app.config.core/app-config]
                                   :config {:conform-rules {#"System\.Switch\..+" :edn
                                                            #".+\.Password" :secret
                                                            #".+\.Secret" :secret
                                                            #"Webapp\.Hosts\(.+\)" :set
                                                            "Development.DatabaseMigration" :edn}
                                            :prop-defaults {"HttpServer.Port" 8080}}}

   ::data-source-read-write {:as ::hikari-data-source
                             :mounts [:app.database.core/data-source-read-write
                                      :app.database.hugsql/data-source-read-write]}

   ::data-source-read-only {:as ::hikari-data-source
                            :mounts [:app.database.core/data-source-read-only
                                     :app.database.hugsql/data-source-read-only]
                            :config {:read-only true}}

   :app.system.task/database-migration {:config {:data-source (ig/ref ::data-source-read-write)
                                                 :changelog-path "app/database/migration/changelog.xml"
                                                 :system-is-enabled true}
                                        :import {:system-is-enabled "Development.DatabaseMigration"}
                                        :mixins [::add-to-await-before-start]}

   :app.system.service/immutant-web {:as ::http-server
                                     :webapps {:app.system.service/homepage-http-handler {:config {:name "example"}
                                                                                          :import {:hosts "Webapp.Hosts(example)"}}}
                                     :config {:options {:host "0.0.0.0"}}
                                     :import {:options {:host "HttpServer.Host"
                                                        :port "HttpServer.Port"}}}})

(defn- system-config
  "App system configuration."
  []
  (->> (system-keys)
       (reduce-kv install {})))

(comment
  (system-config)
  (->> (system-config) keys)
  (->> (system-config) keys (map ig/decompose-key) sort)
  (-> (system-config) (get :app.system.service/mount) keys sort)
  (-> (system-config) ::await-before-start))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defonce ^{:doc "Global reference to the running system"
           :private true}
  !system (atom nil))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn stop!
  "Stop global system."
  []
  (when-some [system @!system]
    (reset! !system nil)
    (ig/halt! system)
    (log/info "[DONE] Application system stop")))

(defn suspend!
  "Suspend global system."
  []
  (some-> @!system (ig/suspend!)))

(defn start!
  "Start global system."
  ([]
   (start! {}))
  ([{:keys [system-keys, prepare-config]
     :or {prepare-config identity}}]
   (stop!)
   (let [config (prepare-config (system-config))]
     (reset! !system (ig/init config, (or system-keys (keys config)))))
   (log/info "[DONE] Application system start")))

(defn resume!
  "Resume global system."
  ([]
   (resume! {}))
  ([{:keys [system-keys, prepare-config]
     :or {prepare-config identity} :as options}]
   (if-some [system @!system]
     (do
       (reset! !system nil)
       (let [config (prepare-config (system-config))]
         (reset! !system (ig/resume config, system, (or system-keys (keys system))))))
     (start! options))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- log-running-webapps
  "Log info about running webapps (URLs with host/port)."
  [system]
  (let [webapps (some-> system :app.system.service/immutant-web e/unwrap-future meta :running-webapps)]
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
  (let [prop-files (some-> system :app.system.service/app-config meta :prop-files)]
    (log/info "Running config from" (pr-str prop-files))))

(add-watch !system :log-system-status
           (fn [_ _ _ system]
             (some-> system (doto (log-prop-files)
                                  (log-running-webapps)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
