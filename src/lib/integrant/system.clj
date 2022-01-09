(ns lib.integrant.system
  (:require [integrant.core :as ig]
            [lib.clojure.core :as c]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key ::identity [_ v] v)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- import-map
  [m from]
  (->> m (into {} (keep (fn [[k v]] (cond (map? v),,,,,,,,,, [k (import-map v from)]
                                          (fn? v),,,,,,,,,,, [k (v from)]
                                          (contains? from v) [k (from v)]))))))

(defmethod ig/init-key ::import-map
  [_ {:keys [import-from import-keys init-map]}]
  (c/deep-merge (or init-map {})
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

(defn simple-key
  "Returns key or the last component of composite key."
  [k]
  (cond-> k (vector? k) (peek)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn get-key-method
  "Given an integrant key multimethod and a dispatch value, returns the dispatch
  fn that would apply to that value, or nil if none apply and no default."
  [f k]
  (get-method f (#'ig/normalize-key k)))

(defn get-defined-key-method
  "Given an integrant key multimethod and a dispatch value, returns the dispatch
  fn that would apply to that value, or nil if none or default apply."
  [f k]
  (let [method (get-key-method f k)]
    (when (and method (not= method (get-method f :default)))
      method)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn ex-failed-system
  "Returns system attached to the integrant build exception e."
  [e]
  (let [system (-> e ex-data :system)]
    (when (and (map? system) (some-> system meta ::ig/origin))
      system)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
