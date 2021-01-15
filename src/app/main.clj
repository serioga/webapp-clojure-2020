(ns app.main
  "Initial namespace for release application.
   Affected in development mode!
   See `dev` namespace as initial for development."
  (:require
    [app.lib.util.exec :as e]
    [app.system.core :as app]
    [clojure.tools.logging :as log])
  (:import
    (org.slf4j.bridge SLF4JBridgeHandler))
  (:gen-class
    :implements [org.apache.commons.daemon.Daemon]))

(set! *warn-on-reflection* true)


(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ _ ex]
      (e/log-error ex))))

(SLF4JBridgeHandler/removeHandlersForRootLogger)
(SLF4JBridgeHandler/install)


(defn- init
  []
  (app/start!)
  (log/info "[DONE] Application init"))

(defn- shutdown
  []
  (app/stop!))

(defn -main
  "Application entry point."
  []
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable shutdown))
  (init))


;------------------------------------------------------------------------------
; Daemon implementation

(defn -init
  "Initializes this `Daemon` instance."
  [_ _])

(defn -start
  "Starts the operation of this `Daemon` instance."
  [_]
  (init))

(defn -stop
  "Stops the operation of this `Daemon` instance."
  [_]
  (shutdown))

(defn -destroy
  "Frees any resources allocated by this daemon."
  [_])
