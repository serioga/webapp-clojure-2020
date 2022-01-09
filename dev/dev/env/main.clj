(ns dev.env.main
  "Initial namespace for development.
   Not included to release application!
   See `core` namespace as initial release application."
  (:require [dev.env.system.app :as app.system]
            [dev.env.system.core :as env.system]
            [lib.clojure-tools-logging.logger :as logger]
            [lib.clojure.core :as c]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private logger (logger/get-logger *ns*))

(defn- init
  []
  (try
    (try (env.system/start)
         (catch Throwable e (throw (->> e (ex-info "Start environment" {:reason ::env})))))

    (try (app.system/start)
         (catch Throwable e (throw (->> e (ex-info "Start application" {:reason ::app})))))

    (when-some [server (env.system/nrepl-server)]
      (logger/info logger (c/pr-str* "Running nREPL server on port" (:port server))))

    (logger/info logger "[DONE] Application has been started for development. Happy coding!")

    (env.system/prompt-reload-on-enter)

    (catch Throwable e
      (logger/error logger (c/ex-message-all e))
      (when (env.system/nrepl-server)
        (env.system/prompt-reload-on-enter)))))

(defn- shutdown
  "Shutdown `env` system."
  []
  (app.system/stop)
  (env.system/stop))

(comment
  (time (init))
  (time (shutdown)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn -main
  "Runs development environment."
  []
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable shutdown))
  (init))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
