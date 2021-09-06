(ns lib.integrant.system
  (:require [integrant.core :as ig]
            [lib.clojure-tools-logging.logger :as logger]
            [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key ::identity [_ v] v)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

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

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key ::system-property
  [_ {k :key default :default}]
  (System/getProperty k default))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

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

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn await-before-start
  "Wait for start of all deferred components listed in `await-for`."
  [map-of-await-for]
  (when (seq map-of-await-for)
    (let [logger (logger/get-logger *ns*)]
      (logger/debug logger (str "Await before start " (keys map-of-await-for)))
      (doseq [[k v] map-of-await-for]
        (try
          (when (future? v) (deref v))
          (catch Throwable t
            (logger/log-throwable logger t (str "Await for " k))))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
