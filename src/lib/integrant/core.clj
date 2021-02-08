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

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

;; Workaround for name resolution in Cursive
;; https://github.com/cursive-ide/cursive/issues/2411
(declare ref)

(import-vars [integrant.core ref])

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn decompose-key
  "Returns key or the last component of composite key."
  [key]
  (cond-> key (vector? key) (peek)))

(defn get-init-key
  "Finds `ig/init-key` defined for `key`."
  [key]
  (get-method ig/init-key (cond-> key (vector? key) (nth 0))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- init-key
  "Wrapped version of integrant's `init-key` with logging."
  [key value]
  (mdc/with-map {:init key}
    (log/info ">> starting.." key)
    (ig/init-key key value)))

(defn- resume-key
  "Wrapped version of integrant's `resume-key` with logging."
  [key value old-value old-impl]
  (mdc/with-map {:resume key}
    (log/info ">> resuming.." key)
    (ig/resume-key key value old-value old-impl)))

(defn- not-default-halt-key?
  [f]
  (not= f (get-method ig/halt-key! :default)))

(defn- not-default-suspend-key?
  [f]
  (not= f (get-method ig/suspend-key! :default)))

(defn- fn'halt-key!
  "Produce wrapped version of integrant's halt-key!
   with logging and handling of returned futures."
  [var'futures]
  (fn halt-key!
    [key value]
    (when-some [method (-> (get-method ig/halt-key! (#'ig/normalize-key key))
                           (e/test-pred not-default-halt-key?))]
      (mdc/with-map {:halt key}
        (log/info ">> stopping.." key)
        (e/try-ignore
          ; Wait for future values to complete.
          ; Ignore errors, they are reported by `init`.
          (when (future? value)
            (deref value))
          (let [ret (e/try-log-error ["Stopping" key]
                      (method key value))]
            (when (future? ret)
              (swap! var'futures conj [key ret]))
            ret))))))

(defn- fn'suspend-key!
  "Produce wrapped version of integrant's suspend-key!
   with logging and handling of returned futures."
  [var'futures]
  (fn suspend-key!
    [key value]
    (when-some [method (or (-> (get-method ig/suspend-key! (#'ig/normalize-key key))
                               (e/test-pred not-default-suspend-key?))
                           (-> (get-method ig/halt-key! (#'ig/normalize-key key))
                               (e/test-pred not-default-halt-key?)))]
      (mdc/with-map {:suspend key}
        (log/info ">> suspending.." key)
        (e/try-ignore
          ; Wait for future values to complete.
          ; Ignore errors, they are reported by `init`.
          (when (future? value)
            (deref value))
          (let [ret (e/try-log-error ["Suspending" key]
                      (method key value))]
            (when (future? ret)
              (swap! var'futures conj [key ret]))
            ret))))))

(defn- ex-in-future
  "Unwrap exception from deref'ed future."
  [ex]
  (or (ex-cause ex) ex))

(defn- collect-failed-futures
  "Scan `system` for futures and collect keys with exceptions."
  [system]
  (reduce (fn [failed [key state]]
            (if (future? state)
              (try
                (deref state)
                failed
                (catch Throwable ex
                  (conj failed [key (ex-in-future ex)])))
              failed))
          (list) system))

(defn- await-build-futures
  "Deref all future key values.
   If there are failed futures then log errors and throw exception to halt system back."
  [system, log-key-error]
  (when-some [failed (seq (collect-failed-futures system))]
    (doseq [[key ex] failed]
      (log-key-error ex key))
    (let [failed-keys (map first failed)]
      (throw (e/ex-info ["Errors on keys" failed-keys "when building system"]
                        {:reason ::failed-futures
                         :system system
                         :failed-keys failed-keys})))))

(defn- await-futures
  "Deref all future suspend results.
   Log errors for failed exceptions."
  [futures, log-key-error]
  (doseq [[key ref'future] futures]
    (try
      (deref ref'future)
      (catch Throwable ex
        (log-key-error (ex-in-future ex) key)))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn halt!
  "Halt a system map by applying halt-key! in reverse dependency order.
   Replacement of integrant's `halt!` with customized `halt-key!` function and handling of futures."
  ([system]
   (halt! system (keys system)))
  ([system keys]
   {:pre [(map? system) (some-> system meta ::ig/origin)]}
   (let [var'futures (atom [])]
     (ig/reverse-run! system keys (fn'halt-key! var'futures))
     (await-futures @var'futures (fn [ex key]
                                   (mdc/with-map {:halt key}
                                     (e/log-error ex "Stopping" key)))))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn suspend!
  "Suspend a system map by applying suspend-key! in reverse dependency order.
   Replacement of integrant's `suspend!` with customized `suspend-key!` function and handling of futures."
  ([system]
   (suspend! system (keys system)))
  ([system keys]
   {:pre [(map? system) (some-> system meta ::ig/origin)]}
   (let [var'futures (atom [])]
     (ig/reverse-run! system keys (fn'suspend-key! var'futures))
     (await-futures @var'futures (fn [ex key]
                                   (mdc/with-map {:suspend key}
                                     (e/log-error ex "Suspending" key)))))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn build
  "Replacement of integrant's `build` with
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
       (let [{:keys [reason, system, key, failed-keys]} (ex-data ex)
             cause (ex-cause ex)]
         (cond
           ; Integrant's exception on non-future key
           (= reason ::ig/build-threw-exception)
           (do
             (log-key-error cause key)
             (some-> system halt!))
           ; Exception after failed future keys
           (= reason ::failed-futures)
           (some-> system
                   (halt! (remove (set failed-keys) (keys system))))
           ; Unexpected exception
           :else
           (log/error "Unexpected error when building system:" (e/ex-message-all ex)))
         (throw ex))))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn init
  "Turn a config map into an system map. Keys are traversed in dependency
   order, initiated via the init-key multimethod, then the refs associated with
   the key are expanded.
   Replacement of integrant's `init` with
   - customized `init-key` function;
   - handling of futures;
   - rollback on exceptions."
  ([config]
   (init config (keys config)))
  ([config system-keys]
   {:pre [(map? config)]}
   (build config system-keys init-key
          (fn [ex key]
            (mdc/with-map {:init key}
              (e/log-error ex "Starting" key)))
          #'ig/assert-pre-init-spec)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn resume
  "Turn a config map into a system map, reusing resources from an existing
   system when it's possible to do so. Keys are traversed in dependency order,
   resumed with the resume-key multimethod, then the refs associated with the
   key are expanded.
   Replacement of integrant's `resume` with
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
          (fn [ex key]
            (mdc/with-map {:resume key}
              (e/log-error ex "Resuming" key))))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
