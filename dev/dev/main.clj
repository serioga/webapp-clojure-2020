(ns dev.main
  "Initial namespace for development.
   Not included to release application!
   See `core` namespace as initial release application."
  (:require
    [app.lib.util.exec :as e]
    [clojure.tools.logging :as log]
    [dev.app.system.core :as dev]
    [dev.app.system.wrap :as app]))

(set! *warn-on-reflection* true)


(defn- init
  []
  (try
    (e/try-wrap-ex ["Start development system" {:reason ::dev}]
      (dev/start!))

    (e/try-wrap-ex ["Start application" {:reason ::app}]
      (app/start!))

    (when-some [server (dev/nrepl-server)]
      (log/info "Running nREPL server on port" (:port server)))

    (log/info "[DONE] Application has been started for development. Happy coding!")

    (dev/reload-on-enter)

    (catch Throwable ex
      (log/error (e/ex-message-all ex))
      (when (= ::app (:reason (ex-data ex)))
        (dev/reload-on-enter)))))


(defn shutdown
  "Shutdown `dev-system`."
  []
  (app/stop!)
  (dev/stop!))


(defn reload
  "Reload `dev-system`."
  []
  (app/stop!)
  (app/start!))


(defn -main
  "Entry point for development run."
  []
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable shutdown))
  (init))


(comment
  (time (init))
  (time (shutdown)))
