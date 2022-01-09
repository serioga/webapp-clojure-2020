(ns app.system.integrant
  (:require [integrant.core :as ig]
            [lib.clojure-tools-logging.logger :as logger]
            [lib.integrant.async :as ig.async]
            [lib.integrant.system :as ig.system]
            [lib.slf4j.mdc :as mdc]
            [lib.util.ansi-escape :as ansi]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- init-key-fn
  "Returns wrapped version of the `integrant.core/init-key` with logging. To use
  with `with-redefs`."
  []
  (let [ig-init-key ig/init-key
        logger (logger/get-logger *ns*)]
    (fn [k v]
      (with-open [_ (mdc/put-closeable "integrant" (str ['start (ig.system/simple-key k)]))]
        (logger/info logger (str ansi/fg-green ">> starting.. " ansi/reset (ig.system/simple-key k)))
        (ig-init-key k v)))))

(defn- halt-key-fn
  "Returns wrapped version of the `integrant.core/halt-key!` with logging and
  exception handling. To use with `with-redefs`."
  []
  (let [ig-halt-key! ig/halt-key!
        logger (logger/get-logger *ns*)]
    (fn [k v]
      (when-let [method (ig.system/get-defined-key-method ig-halt-key! k)]
        (with-open [_ (mdc/put-closeable "integrant" (str ['stop (ig.system/simple-key k)]))]
          (logger/info logger (str ansi/fg-cyan ">> stopping.. " ansi/reset (ig.system/simple-key k)))
          (try
            (method k v)
            (catch Throwable e
              (logger/log-throwable logger e (str "Stopping " (ig.system/simple-key k))))))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn halt!
  "Halts system map asynchronously."
  [system]
  (with-redefs [ig/halt-key! (halt-key-fn)]
    (ig.async/halt! system)))

(defn suspend!
  "Suspends system map asynchronously."
  [system]
  (with-redefs [ig/halt-key! (halt-key-fn)]
    (ig.async/suspend! system)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- build-apply
  [init-fn & args]
  (try
    (with-redefs [ig/init-key (init-key-fn)]
      (apply init-fn args))
    (catch Exception e
      (logger/log-throwable e "Build integrant system")
      (some-> (ig.system/ex-failed-system e)
              (halt!))
      (throw e))))

(defn init
  "Initializes integrant system asynchronously."
  [config ks]
  (build-apply ig.async/init config ks))

(defn resume
  "Resumes integrant system asynchronously."
  [config system ks]
  (build-apply ig.async/resume config system ks))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
