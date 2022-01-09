(ns app.system.config
  (:require [integrant.core :as ig]
            [lib.clojure.core :as e]
            [lib.integrant.system :as ig.system]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(derive ::dev-mode ::ig.system/identity)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn suffix-key
  "Adds 'extension' to keyword `key`."
  [k ext]
  (let [k (ig.system/simple-key k)]
    (keyword (namespace k) (str (name k) ext))))

(defn mount-refs
  "Creates references from the vector of `mounts` keywords to the key `k` in the
  :app.system.service/mount."
  [m k mounts]
  (reduce (fn [m mount-key]
            (update m :app.system.service/mount assoc mount-key (ig/ref k)))
          m mounts))

(defmulti mixin
  "Resolves mixin represented by keyword. By default, returns value as is."
  identity)

(e/add-method mixin :default identity)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- merge-mixins
  [{:keys [mixins] :as params}]
  (if mixins
    (apply e/deep-merge (dissoc params :mixins)
           (map #(-> (mixin %) (e/assert map? "Mixin should be a map"))
                (-> mixins (e/assert sequential? "Require :mixins to be a sequence"))))
    params))

(defn- install-awaits
  "Adds waiting dependencies from :awaits param to key config."
  [config {:keys [awaits]}]
  (cond-> config
    awaits (assoc ::awaits (into {} (map (juxt identity ig/ref)) awaits))))

(defn- import-app-config
  "Creates a pair of keys for service `k` and its configuration `config-k`
  imported from app-config. Also mounts references for optional vector of
  `mounts` and installs `awaits` to the config."
  [m, k {:keys [config] import-keys :import :as params}]
  (let [config-k (cond-> k (ig.system/get-key-method ig/init-key k) (suffix-key ".config"))]
    (cond-> (assoc m [::ig.system/import-map config-k] {:init-map (-> (or config {})
                                                                      (install-awaits params))
                                                        :import-from (ig/ref :app.system.service/app-config)
                                                        :import-keys import-keys})
      (not= k config-k) (assoc k (ig/ref config-k)))))

(defn install
  "Default `install` behaviour."
  {:arglists '([config-map, key, {:keys [derive, config, import, mounts, mixins, awaits]}])}
  [m, k, params]
  (let [{:keys [config, mounts] derive-k :derive :as params} (merge-mixins params)
        k (cond->> k derive-k (vector derive-k))]
    (cond-> (if (:import params) (import-app-config m k params)
                                 (assoc m k (-> config (install-awaits params))))
      mounts (mount-refs k mounts))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmulti install-as
  "Adds keys to the config map for `key` with specific :as in params."
  {:arglists '([config-map, key, {:keys [as, derive, config, import, mounts, mixins, awaits]}])}
  (fn [_ _ {:keys [as]}] as))

(e/add-method install-as nil install)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(derive :dev.env.system/prepare-prop-files ::ig.system/identity)

(defmethod install-as ::app-config
  [m k {:keys [config mounts]}]
  (let [prop-files (suffix-key k ".prop-files")]
    (-> m (merge {k (merge {:prop-files (ig/ref prop-files)
                            :dev/prepare-prop-files (ig/ref :dev.env.system/prepare-prop-files)}
                           config)
                  [::ig.system/system-property prop-files] {:key "config.file"}
                  :dev.env.system/prepare-prop-files nil})
        (mount-refs k mounts))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod install-as ::hikari-data-source
  [m, k, params]
  (install m k (e/deep-merge params {:derive :app.system.service/hikari-data-source
                                     :config {:dev-mode (ig/ref ::dev-mode)}
                                     :import {:data-source-class "Database.DataSourceClassName"
                                              :database-url (if (-> params :config :read-only)
                                                              "Database.Url.ReadOnly"
                                                              "Database.Url")
                                              :database-user "Database.User"
                                              :database-password "Database.Password"}})))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(derive :dev.env.system/prepare-webapp ::ig.system/identity)

(defmethod install-as ::http-server
  [m, k, {:keys [webapps] :as params}]
  (-> (reduce (fn [system [k params]]
                (install-as system [:app.system.service/webapp-http-handler k]
                            (update params :config merge {:dev-mode (ig/ref ::dev-mode)})))
              m webapps)
      (assoc :dev.env.system/prepare-webapp nil)
      (install k (update params :config merge {:webapps (->> webapps (mapv (comp ig/ref first)))
                                               :dev/prepare-webapp (ig/ref :dev.env.system/prepare-webapp)}))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn build-config
  "Returns integrant config map applying `install` across all keys in the
  config map template."
  [template]
  (reduce-kv install-as {} template))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
