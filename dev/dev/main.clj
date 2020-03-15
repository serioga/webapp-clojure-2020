(ns dev.main
  "Initial namespace for development.
   Not included to release application!
   See `core` namespace as initial release application."
  (:require
    [app.lib.util.exec :as e]
    [clojure.tools.logging :as log]
    [dev.dev-system.app-system :as app-system]
    [dev.dev-system.core :as dev-system]))

(set! *warn-on-reflection* true)


(defn- init
  []
  (try
    (e/try-wrap-ex ["Start development system" {:reason ::dev-system}]
      (dev-system/start!))

    (e/try-wrap-ex ["Start application" {:reason ::app-system}]
      (app-system/start!))

    (when-some [server (dev-system/nrepl-server)]
      (log/info "Running nREPL server on port" (:port server)))

    (log/info "[DONE] Application has been started for development. Happy coding!")

    (dev-system/reload-on-enter)

    (catch Throwable ex
      (log/error (e/ex-message-all ex))
      (when (= ::app-system (:reason (ex-data ex)))
        (dev-system/reload-on-enter)))))


(defn shutdown
  "Shutdown `dev-system`."
  []
  (app-system/stop!)
  (dev-system/stop!))


(defn reload
  "Reload `dev-system`."
  []
  (app-system/stop!)
  (app-system/start!))


(defn -main
  "Entry point for development run."
  []
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable shutdown))
  (init))


(comment
  (time (init))
  (time (shutdown)))
