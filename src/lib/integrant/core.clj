(ns lib.integrant.core
  "Adapted integrant functionality.
   Features:
   - system rollback on failures;
   - futures in `init-key` and `halt-key!` for parallel initialization."
  (:refer-clojure :exclude [ref])
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [lib.clojure.core :as e]
            [lib.slf4j.mdc :as mdc]
            [potemkin :refer [import-vars]]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

;; Workaround for name resolution in Cursive.
;; See https://github.com/cursive-ide/cursive/issues/2411.
(declare ref)

(import-vars [integrant.core ref])

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn decompose-key
  "Returns key or the last component of composite key."
  [k]
  (cond-> k (vector? k) (peek)))

(defn get-init-key
  "Finds `ig/init-key` defined for `key`."
  [k]
  (get-method ig/init-key (cond-> k (vector? k) (nth 0))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- init-key
  "Wrapped version of the `integrant.core/init-key` with logging."
  [k value]
  (mdc/with-map {:init k}
    (log/info ">> starting.." k)
    (ig/init-key k value)))

(defn- resume-key
  "Wrapped version of the `integrant.core/resume-key` with logging."
  [k value old-value old-impl]
  (mdc/with-map {:resume k}
    (log/info ">> resuming.." k)
    (ig/resume-key k value old-value old-impl)))

(defn- not-default-halt-key?
  [f]
  (not= f (get-method ig/halt-key! :default)))

(defn- not-default-suspend-key?
  [f]
  (not= f (get-method ig/suspend-key! :default)))

(defn- fn'halt-key!
  "Produce wrapped version of the `integrant.core/halt-key!`
   with logging and handling of returned futures."
  [!futures]
  (fn halt-key!
    [k value]
    (when-some [method (-> (get-method ig/halt-key! (#'ig/normalize-key k))
                           (e/asserted not-default-halt-key?))]
      (mdc/with-map {:halt k}
        (log/info ">> stopping.." k)
        (e/try-ignore
          ;; Wait for future values to complete.
          ;; Ignore errors, they are reported by `init`.
          (when (future? value)
            (deref value))
          (let [ret (e/try-log-error ["Stopping" k]
                      (method k value))]
            (when (future? ret)
              (swap! !futures conj [k ret]))
            ret))))))

(defn- fn'suspend-key!
  "Produce wrapped version of the `integrant.core/suspend-key!`
   with logging and handling of returned futures."
  [!futures]
  (fn suspend-key!
    [k value]
    (when-some [method (or (-> (get-method ig/suspend-key! (#'ig/normalize-key k))
                               (e/asserted not-default-suspend-key?))
                           (-> (get-method ig/halt-key! (#'ig/normalize-key k))
                               (e/asserted not-default-halt-key?)))]
      (mdc/with-map {:suspend k}
        (log/info ">> suspending.." k)
        (e/try-ignore
          ;; Wait for future values to complete.
          ;; Ignore errors, they are reported by `init`.
          (when (future? value)
            (deref value))
          (let [ret (e/try-log-error ["Suspending" k]
                      (method k value))]
            (when (future? ret)
              (swap! !futures conj [k ret]))
            ret))))))

(defn- ex-in-future
  "Unwrap exception from the Future."
  [ex]
  (or (ex-cause ex) ex))

(defn- await-build-futures
  "Deref all future key values.
   If there are failed futures then log errors and throw exception to halt system back."
  [system, log-key-error]
  (doseq [[k v] system :when (future? v)]
    (try
      (deref v)
      (catch Throwable ex
        (let [ex (ex-in-future ex)]
          (log-key-error ex k)
          (throw (#'ig/build-exception system nil k v ex)))))))

(defn- await-futures
  "Deref all future suspend results.
   Log errors for failed exceptions."
  [futures, log-key-error]
  (doseq [[k ?future] futures]
    (try
      (deref ?future)
      (catch Throwable ex
        (log-key-error (ex-in-future ex) k)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn halt!
  "Halt a system map by applying halt-key! in reverse dependency order.
   Replacement of the `integrant.core/halt!` with customized `halt-key!` function and handling of futures."
  ([system]
   (halt! system (keys system)))
  ([system ks]
   {:pre [(map? system) (some-> system meta ::ig/origin)]}
   (let [!futures (atom [])]
     (ig/reverse-run! system ks (fn'halt-key! !futures))
     (await-futures @!futures (fn [ex k]
                                (mdc/with-map {:halt k}
                                  (e/log-error ex "Stopping" k)))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn suspend!
  "Suspend a system map by applying suspend-key! in reverse dependency order.
   Replacement of the `integrant.core/suspend!` with customized `suspend-key!` function and handling of futures."
  ([system]
   (suspend! system (keys system)))
  ([system ks]
   {:pre [(map? system) (some-> system meta ::ig/origin)]}
   (let [!futures (atom [])]
     (ig/reverse-run! system ks (fn'suspend-key! !futures))
     (await-futures @!futures (fn [ex k]
                                (mdc/with-map {:suspend k}
                                  (e/log-error ex "Suspending" k)))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn build
  "Replacement of the `integrant.core/build` with
   - handling of futures;
   - rollback on exceptions."
  ([config, system-keys, f, log-key-error]
   (build config, system-keys, f, log-key-error, (fn [_ _ _])))
  ([config, system-keys, f, log-key-error, assert-fn]
   (try
     (let [system (ig/build config system-keys f assert-fn)]
       (await-build-futures system log-key-error)
       system)
     (catch Throwable ex
       (when-let [data (ex-data ex)]
         (when (= (:reason data) ::ig/build-threw-exception)
           (log-key-error (ex-cause ex) (:key data))
           (some-> (:system data) halt!))
         (throw ex))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn init
  "Turn a config map into an system map. Keys are traversed in dependency
   order, initiated via the init-key multimethod, then the refs associated with
   the key are expanded.
   Replacement of the `integrant.core/init` with
   - customized `init-key` function;
   - handling of futures;
   - rollback on exceptions."
  ([config]
   (init config (keys config)))
  ([config system-keys]
   {:pre [(map? config)]}
   (build config system-keys init-key
          (fn [ex k]
            (mdc/with-map {:init k}
              (e/log-error ex "Starting" k)))
          #'ig/assert-pre-init-spec)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn resume
  "Turn a config map into a system map, reusing resources from an existing
   system when it's possible to do so. Keys are traversed in dependency order,
   resumed with the resume-key multimethod, then the refs associated with the
   key are expanded.
   Replacement of the `integrant.core/resume` with
   - customized `resume-key` function;
   - handling of futures;
   - rollback on exceptions."
  ([config system]
   (resume config system (keys config)))
  ([config system system-keys]
   {:pre [(map? system) (some-> system meta ::ig/origin)]}
   (e/try-log-error "Halt missing keys on resume"
     (#'ig/halt-missing-keys! config system system-keys))
   (build config system-keys
          (fn [k v] (if (contains? system k)
                      (resume-key k v (-> system meta ::ig/build (get k)) (system k))
                      (init-key k v)))
          (fn [ex k]
            (mdc/with-map {:resume k}
              (e/log-error ex "Resuming" k))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
