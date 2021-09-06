(ns dev.env.main
  "Initial namespace for development.
   Not included to release application!
   See `core` namespace as initial release application."
  (:require [dev.env.system.app :as app]
            [dev.env.system.core :as env]
            [lib.clojure-tools-logging.logger :as logger]
            [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private logger (logger/get-logger *ns*))

(defn- init
  []
  (try
    (try (env/start)
         (catch Throwable t (throw (ex-info "Start environment" {:reason ::env} t))))

    (try (app/start)
         (catch Throwable t (throw (ex-info "Start application" {:reason ::app} t))))

    (when-some [server (env/nrepl-server)]
      (logger/info logger (e/p-str "Running nREPL server on port" (:port server))))

    (logger/info logger "[DONE] Application has been started for development. Happy coding!")

    (env/prompt-reload-on-enter)

    (catch Throwable ex
      (logger/error logger (e/ex-message-all ex))
      (when (= ::app (:reason (ex-data ex)))
        (env/prompt-reload-on-enter)))))

(defn- shutdown
  "Shutdown `env` system."
  []
  (app/stop)
  (env/stop))

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
