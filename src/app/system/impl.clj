(ns app.system.impl
  "Service functions to implement app system."
  (:require
    [clojure.tools.logging :as log]
    [integrant.core :as ig]
    [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn log-running-webapps
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

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn log-prop-files
  "Log info about loaded configuration files."
  [system]
  (let [prop-files (some-> system
                           :app.system.service/app-config
                           (meta)
                           :prop-files)]
    (log/info "Running config from" (pr-str prop-files))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key ::identity [_ v] v)

(derive :app.system/dev-mode? ::identity)
(derive :dev.env.system/prepare-prop-files ::identity)
(derive :dev.env.system/prepare-webapp ::identity)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- import-map
  [m from]
  (into {}
        (keep (fn [[k v]] (cond
                            (map? v) [k (import-map v from)]
                            (fn? v) [k (v from)]
                            :else (when (contains? from v)
                                    [k (from v)])))
              m)))

(defmethod ig/init-key ::import-map
  [_ {:keys [import-from import-keys init-map]}]
  (e/deep-merge (or init-map {})
                (import-map import-keys import-from)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key ::system-property
  [_ {:keys [key default]}]
  (System/getProperty key default))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- restart-on-resume
  [old-impl k value]
  (ig/halt-key! k old-impl)
  (ig/init-key k value))

(defmethod ig/suspend-key! ::keep-running-on-suspend
  [_ _])

(defmethod ig/resume-key ::keep-running-on-suspend
  [k value old-value old-impl]
  (cond-> old-impl
    (not= value old-value) (restart-on-resume k value)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn await-before-start
  "Wait for start of all deferred components listed in `await-for`."
  [await-for]
  (when (seq await-for)
    (log/debug "Await before start" (keys await-for))
    (doseq [[k v] await-for]
      (e/try-log-error ["Await for" k]
        (when (future? v)
          (deref v))))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
