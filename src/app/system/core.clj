(ns app.system.core
  (:require [lib.clojure-tools-logging.logger :as logger]
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

(def ^:private logger (logger/get-logger *ns*))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- key-with-suffix
  "Adds 'extension' to keyword `key`."
  [k ext]
  (let [k (ig/decompose-key k)]
    (keyword (namespace k) (str (name k) ext))))

(defn- add-to-await-before-start
  [system k]
  (let [k (ig/decompose-key k)]
    (update system ::await-before-start assoc k (ig/ref k))))

(defn- mount-refs
  "Creates references from vector of `mounts` keywords to the key `k`
   in `:app.system.service/mount`.
   If `k` is composite then only the last value is taken."
  [system k mounts]
  (let [k (ig/decompose-key k)]
    (reduce (fn [system mount-key]
              (-> system
                  (update :app.system.service/mount assoc mount-key (ig/ref k))
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
  [system, k, params]
  (install system, k, (dissoc params :as)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- merge-mixins
  [{:keys [mixins] :as params}]
  (if mixins (apply e/deep-merge (dissoc params :mixins)
                    (map #(-> (mixin %) (e/assert map? "Mixin should be a map"))
                         (-> mixins (e/assert sequential? "Require :mixins to be a sequence"))))
             params))

(defn- import-app-config
  "Creates pair of keys for service `k` and its configuration `config-k` imported from app-config.
   Also mounts references for optional vector of `mounts`."
  [system, k {:keys [config] import-keys :import}]
  (let [config-k (cond-> k (ig/get-init-key k) (key-with-suffix ".config"))]
    (cond-> (assoc system [::system/import-map config-k] {:init-map (or config {})
                                                          :import-from (ig/ref :app.system.service/app-config)
                                                          :import-keys import-keys})
      (not= k config-k) (assoc k (ig/ref config-k)))))

(defmethod install nil
  [system, k, params]
  (let [{:keys [config, mounts] derive-k :derive :as params} (merge-mixins params)
        k (cond->> k derive-k (vector derive-k))]
    (cond-> (if (:import params) (import-app-config system k params)
                                 (assoc system k config))
      mounts,,,,,,,,,,,,,,,,,,,,,,,,,,,,,, (mount-refs k mounts)
      (::add-to-await-before-start params) (add-to-await-before-start k))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod install ::app-config
  [system k {:keys [config mounts]}]
  (let [prop-files (key-with-suffix k ".prop-files")]
    (-> system (merge {:dev.env.system/prepare-prop-files nil

                       [::system/system-property prop-files] {:key "config.file"}

                       k (merge {:prop-files (ig/ref prop-files)
                                 :dev/prepare-prop-files (ig/ref :dev.env.system/prepare-prop-files)}
                                config)})
        (mount-refs k mounts))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod install ::hikari-data-source
  [system, k, params]
  (install* system k (e/deep-merge params {:derive :app.system.service/hikari-data-source
                                           :config {:dev-mode (ig/ref ::dev-mode)}
                                           :import {:data-source-class "Database.DataSourceClassName"
                                                    :database-url (if (-> params :config :read-only)
                                                                    "Database.Url.ReadOnly", "Database.Url")
                                                    :database-user "Database.User"
                                                    :database-password "Database.Password"}})))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod install ::http-server
  [system, k, {:keys [webapps] :as params}]
  (-> (reduce (fn [system [k params]]
                (install system [:app.system.service/webapp-http-handler k]
                         (update params :config merge {:dev-mode (ig/ref ::dev-mode)})))
              system webapps)
      (assoc :dev.env.system/prepare-webapp nil)
      (install* k (update params :config merge {:webapps (->> webapps (mapv (comp ig/ref first)))
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

(defn stop
  "Stop global system."
  []
  (when-some [system @!system]
    (reset! !system nil)
    (ig/halt! system)
    (logger/info logger "[DONE] Application system stop")))

(defn suspend
  "Suspend global system."
  []
  (some-> @!system (ig/suspend!)))

(defn start
  "Start global system."
  {:arglists '([] [{:keys [:system-keys :prepare-config]}])}
  ([]
   (start {}))
  ([options]
   (stop)
   (let [config ((:prepare-config options identity) (system-config))]
     (reset! !system (ig/init config, (or (:system-keys options) (keys config)))))
   (logger/info logger "[DONE] Application system start")))

(defn resume
  "Resume global system."
  {:arglists '([] [{:keys [:system-keys :prepare-config]}])}
  ([]
   (resume {}))
  ([options]
   (if-some [system @!system]
     (do
       (reset! !system nil)
       (let [config ((:prepare-config options identity) (system-config))]
         (reset! !system (ig/resume config, system, (or (:system-keys options) (keys system))))))
     (start options))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- log-running-webapps
  "Log info about running webapps (URLs with host/port)."
  [system]
  (let [webapps (some-> system :app.system.service/immutant-web e/unwrap-future meta :running-webapps)]
    (doseq [[webapp-name {:keys [host port ssl-port virtual-host]}] webapps
            webapp-host (cond (sequential? virtual-host) virtual-host
                              (string? virtual-host) [virtual-host]
                              :else [(or host "localhost")])]
      (logger/info logger (print-str "Running webapp" (pr-str webapp-name)
                                     (str (when port (str "- http://" webapp-host ":" port "/")))
                                     (str (when ssl-port (str "- https://" webapp-host ":" ssl-port "/"))))))))

(defn- log-prop-files
  "Log info about loaded configuration files."
  [system]
  (let [prop-files (some-> system :app.system.service/app-config meta :prop-files)]
    (logger/info logger (e/p-str "Running config from" prop-files))))

(add-watch !system :log-system-status
           (fn [_ _ _ system]
             (some-> system (doto (log-prop-files)
                                  (log-running-webapps)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
