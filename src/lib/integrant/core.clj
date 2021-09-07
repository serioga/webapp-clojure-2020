(ns lib.integrant.core
  "Adapted integrant functionality.
   Features:
   - system rollback on failures;
   - futures in `init-key` and `halt-key!` for parallel initialization."
  (:refer-clojure :exclude [ref])
  (:require [integrant.core :as ig]
            [lib.clojure-tools-logging.logger :as logger]
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

(def ^:private logger (logger/get-logger *ns*))

(defn- init-key
  "Wrapped version of the `integrant.core/init-key` with logging."
  [k value]
  (with-open [_ (mdc/put-closeable "integrant" (str ['init-key (decompose-key k)]))]
    (logger/info logger (str ">> starting.. " k))
    (ig/init-key k value)))

(defn- resume-key
  "Wrapped version of the `integrant.core/resume-key` with logging."
  [k value old-value old-impl]
  (with-open [_ (mdc/put-closeable "integrant" (str ['resume-key (decompose-key k)]))]
    (logger/info logger (str ">> resuming.. " k))
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
      (with-open [_ (mdc/put-closeable "integrant" (str ['halt-key! (decompose-key k)]))]
        (logger/info logger (str ">> stopping.. " k))
        (when (future? value)
          (try
            ;; Wait for initialization future values to complete.
            (deref value)
            ;; Ignore errors, they are reported by `init`.
            (catch Throwable _)))
        (let [ret (try (method k value)
                       (catch Throwable e (logger/log-throwable logger e (str "Stopping " k))))]
          (when (future? ret)
            (swap! !futures conj [k ret]))
          ret)))))

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
      (with-open [_ (mdc/put-closeable "integrant" (str ['suspend-key! (decompose-key k)]))]
        (logger/info logger (str ">> suspending.. " k))
        (when (future? value)
          (try
            ;; Wait for initialization future values to complete.
            (deref value)
            ;; Ignore errors, they are reported by `init`.
            (catch Throwable _)))
        (let [ret (try (method k value)
                       (catch Throwable e (logger/log-throwable logger e (str "Suspending " k))))]
          (when (future? ret)
            (swap! !futures conj [k ret]))
          ret)))))

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
      (catch Throwable e
        (let [e (ex-in-future e)]
          (log-key-error k e)
          (throw (#'ig/build-exception system nil k v e)))))))

(defn- await-futures
  "Deref all future suspend results.
   Log errors for failed exceptions."
  [futures, log-key-error]
  (doseq [[k ?future] futures]
    (try
      (deref ?future)
      (catch Throwable e
        (log-key-error k (ex-in-future e))))))

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
     (await-futures @!futures (fn [k throwable]
                                (with-open [_ (mdc/put-closeable "integrant" (str ['halt-key! (decompose-key k)]))]
                                  (logger/log-throwable logger throwable (str "Stopping " k))))))))

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
     (await-futures @!futures (fn [k throwable]
                                (with-open [_ (mdc/put-closeable "integrant" (str ['suspend-key! (decompose-key k)]))]
                                  (logger/log-throwable logger throwable (str "Suspending " k))))))))

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
     (catch Throwable e
       (when-let [data (ex-data e)]
         (when (= (:reason data) ::ig/build-threw-exception)
           (log-key-error (:key data) (ex-cause e))
           (some-> (:system data) halt!))
         (throw e))))))

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
          (fn [k throwable]
            (with-open [_ (mdc/put-closeable "integrant" (str ['init-key (decompose-key k)]))]
              (logger/log-throwable logger throwable (str "Starting " k))))
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
   (try (#'ig/halt-missing-keys! config system system-keys)
        (catch Throwable e (logger/log-throwable logger e "Halt missing keys on resume")))
   (build config system-keys
          (fn [k v] (if (contains? system k)
                      (resume-key k v (-> system meta ::ig/build (get k)) (system k))
                      (init-key k v)))
          (fn [k throwable]
            (with-open [_ (mdc/put-closeable "integrant" (str ['resume-key (decompose-key k)]))]
              (logger/log-throwable logger throwable (str "Resuming " k)))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
