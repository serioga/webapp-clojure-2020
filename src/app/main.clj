(ns app.main
  "Initial namespace for release application.
   Affected in development mode!
   See `dev` namespace as initial for development."
  (:require [app.system.core :as app]
            [clojure.tools.logging :as log]
            [lib.clojure.core :as e])
  (:import (org.slf4j.bridge SLF4JBridgeHandler))
  (:gen-class :implements [org.apache.commons.daemon.Daemon]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ _ ex]
      (e/log-error ex))))

(SLF4JBridgeHandler/removeHandlersForRootLogger)
(SLF4JBridgeHandler/install)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- start
  []
  (try
    (e/try-wrap-ex "[FAIL] Application init"
      (app/start))
    (log/info "[DONE] Application init")
    (catch Throwable ex
      (log/error (e/ex-message-all ex))
      (throw (e/ex-root-cause ex)))))

(defn- stop
  []
  (app/stop))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn -main
  "Application entry point."
  []
  (start)
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable stop)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

;;; Daemon implementation

(defn -init
  "Initializes this `Daemon` instance."
  [_ _])

(defn -start
  "Starts the operation of this `Daemon` instance."
  [_]
  (start))

(defn -stop
  "Stops the operation of this `Daemon` instance."
  [_]
  (stop))

(defn -destroy
  "Frees any resources allocated by this daemon."
  [_])

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
