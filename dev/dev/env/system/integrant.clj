(ns dev.env.system.integrant
  (:require [integrant.core :as ig]
            [lib.clojure-tools-logging.logger :as logger]
            [lib.integrant.system :as ig.system]
            [lib.slf4j.mdc :as mdc]
            [lib.util.ansi-escape :as ansi]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private wrap-key?
  (complement #{:dev.env.system.integrant/watcher}))

(defn- init-key-fn
  "Returns wrapped version of the `integrant.core/init-key` with logging. To use
  with `with-redefs`."
  []
  (let [ig-init-key ig/init-key
        logger (logger/get-logger *ns*)]
    (fn [k v]
      (if (wrap-key? k)
        (with-open [_ (mdc/put-closeable "integrant" (str ['start (ig.system/simple-key k)]))]
          (logger/info logger (str ansi/fg-green-b ">> starting.. " ansi/reset (ig.system/simple-key k)))
          (ig-init-key k v))
        (ig-init-key k v)))))

(defn- halt-key-fn
  "Returns wrapped version of the `integrant.core/halt-key!` with logging and
  exception handling. To use with `with-redefs`."
  []
  (let [ig-halt-key! ig/halt-key!
        logger (logger/get-logger *ns*)]
    (fn [k v]
      (if (wrap-key? k)
        (when-let [method (ig.system/get-defined-key-method ig-halt-key! k)]
          (with-open [_ (mdc/put-closeable "integrant" (str ['stop (ig.system/simple-key k)]))]
            (logger/info logger (str ansi/fg-cyan-b ">> stopping.. " ansi/reset (ig.system/simple-key k)))
            (try
              (method k v)
              (catch Throwable e
                (logger/log-throwable logger e (str "Stopping " (ig.system/simple-key k)))))))
        (ig-halt-key! k v)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn halt!
  "Halts system map."
  [system]
  (with-redefs [ig/halt-key! (halt-key-fn)]
    (ig/halt! system)))

(defn suspend!
  "Suspends system map."
  [system]
  (with-redefs [ig/halt-key! (halt-key-fn)]
    (ig/suspend! system)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn init
  "Initializes integrant system."
  [config]
  (with-redefs [ig/init-key (init-key-fn)]
    (ig/init config)))

(defn resume
  "Resumes integrant system."
  [config system]
  (with-redefs [ig/init-key (init-key-fn)]
    (ig/resume config system)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
