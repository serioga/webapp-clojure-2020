(ns app.main
  "Initial namespace for release application.
   Affected in development mode!
   See `dev` namespace as initial for development."
  (:require
    [app.app-system.core :as app-system]
    [app.lib.util.exec :as exec]
    [clojure.tools.logging :as log])
  (:import
    (org.slf4j.bridge SLF4JBridgeHandler))
  (:gen-class
    :implements [org.apache.commons.daemon.Daemon]))

(set! *warn-on-reflection* true)


(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ _ ex]
      (exec/log-error ex))))

(SLF4JBridgeHandler/removeHandlersForRootLogger)
(SLF4JBridgeHandler/install)


(defn init
  []
  (app-system/start!)
  (log/info "[DONE] Application init"))

(defn shutdown
  []
  (app-system/stop!))

(defn -main
  []
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable shutdown))
  (init))


;------------------------------------------------------------------------------
; Daemon implementation

(defn -init, [_ _])
(defn -start,, [_] (init))
(defn -stop,,, [_] (shutdown))
(defn -destroy [_])
