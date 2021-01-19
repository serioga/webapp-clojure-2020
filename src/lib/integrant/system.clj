(ns lib.integrant.system
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key ::identity [_ v] v)

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
