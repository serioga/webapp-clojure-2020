(ns dev.env.main
  "Initial namespace for development.
   Not included to release application!
   See `core` namespace as initial release application."
  (:require
    [clojure.tools.logging :as log]
    [dev.env.system.app :as app]
    [dev.env.system.core :as env]
    [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- init
  []
  (try
    (e/try-wrap-ex ["Start environment" {:reason ::env}]
      (env/start!))

    (e/try-wrap-ex ["Start application" {:reason ::app}]
      (app/start!))

    (when-some [server (env/nrepl-server)]
      (log/info "Running nREPL server on port" (:port server)))

    (log/info "[DONE] Application has been started for development. Happy coding!")

    (env/prompt-reload-on-enter)

    (catch Throwable ex
      (log/error (e/ex-message-all ex))
      (when (= ::app (:reason (ex-data ex)))
        (env/prompt-reload-on-enter)))))

(defn- shutdown
  "Shutdown `env` system."
  []
  (app/stop!)
  (env/stop!))

(comment
  (time (init))
  (time (shutdown)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn -main
  "Runs development environment."
  []
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable shutdown))
  (init))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
