(ns app.app-system.impl
  "Service functions to implement app-system."
  (:require
    [app.lib.util.exec :as e]
    [clojure.tools.logging :as log]
    [integrant.core :as ig]))

(set! *warn-on-reflection* true)


(defn log-running-webapps
  "Log info about running webapps (URLs with host/port)."
  [system]
  (let [webapps (some-> system
                        :app-system.service/ref'immutant-web
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


(defn log-prop-files
  "Log info about loaded configuration files."
  [system]
  (let [prop-files (some-> system
                           :app-system.service/app-config
                           (meta)
                           :prop-files)]
    (log/info "Running config from" (pr-str prop-files))))


(defn deep-merge
  "https://gist.github.com/danielpcox/c70a8aa2c36766200a95#gistcomment-2759497"
  [a b]
  (if (map? a)
    (merge-with deep-merge a b)
    b))


(defn- import-map
  [m from]
  (into {}
        (keep (fn [[k v]] (cond
                            (map? v) [k (import-map v from)]
                            (fn? v) [k (v from)]
                            :else (when (contains? from v)
                                    [k (from v)])))
              m)))


(defmethod ig/init-key :app-system.core/identity [_ v] v)
(derive :app-system/dev-mode? :app-system.core/identity)
(derive :app-system.dev/prepare-prop-files :app-system.core/identity)
(derive :app-system.dev/prepare-webapp :app-system.core/identity)


(defmethod ig/init-key :app-system.core/init-map
  [_ {:keys [import-from import-keys init-map]}]
  (deep-merge (or init-map {})
              (import-map import-keys import-from)))

(comment
  (deep-merge {:a true} {:a false})
  (ig/init-key :app-system.core/init-map
               {:init-map {:x 0 :c {:x 1 :d true}}
                :import-from {"a" 1 "b" 2 "c.d" false}
                :import-keys {:a "a" :b "b" :c {:d "c.d"} :e "missing" :f (constantly :f)}}))


(defmethod ig/init-key :app-system.core/system-property
  [_ {:keys [key default]}]
  (System/getProperty key default))


(defmethod ig/suspend-key! :app-system.core/keep-running-on-suspend
  [_ _])


(defn- restart-on-resume
  [old-impl k value]
  (ig/halt-key! k old-impl)
  (ig/init-key k value))


(defmethod ig/resume-key :app-system.core/keep-running-on-suspend
  [k value old-value old-impl]
  (cond-> old-impl
    (not= value old-value) (restart-on-resume k value)))


(defn await-before-start
  "Wait for start of all deferred components listed in `await-for`."
  [await-for]
  (when (seq await-for)
    (log/debug "Await before start" (keys await-for))
    (doseq [[k v] await-for]
      (e/try-log-error ["Await for" k]
        (when (future? v)
          (deref v))))))
