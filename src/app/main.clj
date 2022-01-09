(ns app.main
  "Initial namespace for release application.
   Affected in development mode!
   See `dev` namespace as initial for development."
  (:require [app.system.core :as app.system]
            [lib.clojure-tools-logging.logger :as logger])
  (:import (org.slf4j.bridge SLF4JBridgeHandler))
  (:gen-class :implements [org.apache.commons.daemon.Daemon]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ _ e]
      (logger/log-throwable e "UncaughtExceptionHandler"))))

(SLF4JBridgeHandler/removeHandlersForRootLogger)
(SLF4JBridgeHandler/install)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- start
  []
  (try
    (app.system/start)
    (logger/info (logger/get-logger *ns*) "[DONE] Application init")
    (catch Throwable e
      (logger/log-throwable e "[FAIL] Application init")
      (throw e))))

(defn- stop
  []
  (app.system/stop))

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
