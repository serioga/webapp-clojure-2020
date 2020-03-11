(ns app.app-system.impl
  "Service functions to implement app-system."
  (:require
    [app.lib.util.exec :as exec]
    [clojure.tools.logging :as log]
    [integrant.core :as ig]))

(set! *warn-on-reflection* true)


(defn log-running-webapps
  [system]
  (let [webapps (some-> system
                        :app-system.service/ref'immutant-web
                        (deref)
                        (meta)
                        :running-webapps)]
    (doseq [[name {:keys [port ssl-port virtual-host]}] webapps
            host (cond
                   (sequential? virtual-host) virtual-host
                   (string? virtual-host) [virtual-host]
                   :else ["localhost"])]
      (log/info "Running" "webapp" (pr-str name)
                (str (when port (str "- http://" host ":" port "/")))
                (str (when ssl-port (str "- https://" host ":" ssl-port "/")))))))


(defn log-prop-files
  [system]
  (let [prop-files (some-> system
                           :app-system.service/app-config
                           (meta)
                           :prop-files)]
    (log/info "Running config from" (pr-str prop-files))))


(defn log-running-rpc-service
  [system key]
  (let [{:keys [enabled? service rpc]} (some-> system
                                               (get key)
                                               (meta))]
    (when enabled?
      (log/info "Running server" (pr-str service)
                "-" "rpc-server" (pr-str (select-keys rpc [:host :product :instance]))))))


(defn deep-merge
  "https://gist.github.com/danielpcox/c70a8aa2c36766200a95#gistcomment-2759497"
  [a b]
  (if (map? a)
    (merge-with deep-merge a b)
    b))


(defn import-map [m from]
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

#_(comment
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


(defn ^:private restart-on-resume
  [old-impl k value]
  (ig/halt-key! k old-impl)
  (ig/init-key k value))


(defmethod ig/resume-key :app-system.core/keep-running-on-suspend
  [k value old-value old-impl]
  (cond-> old-impl
    (not= value old-value) (restart-on-resume k value)))


(defn await-before-start
  [await-for]
  (when (seq await-for)
    (log/debug "Await before start" (keys await-for))
    (doseq [[k v] await-for]
      (exec/try-log-error ["Await for" k]
        (when (future? v)
          (deref v))))))
