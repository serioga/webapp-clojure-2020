(ns app.system.service.immutant-web
  (:require [clojure.tools.logging :as log]
            [immutant.web :as web]
            [integrant.core :as ig]
            [lib.clojure.core :as e]
            [lib.integrant.system :as system]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- start-webapp
  [server, {:keys [name handler options]}, server-options]
  (let [options (merge server-options options)]
    (log/debug "Start webapp" (pr-str name) (pr-str options))
    (-> (web/run handler (merge server options))
        (with-meta (update (meta server) :running-webapps
                           conj [name options])))))

(defn- skip-webapp
  [server, webapp]
  (log/debug "Skip webapp" (pr-str webapp))
  server)

(defn- start-server
  [{:keys [options
           webapps
           dev/prepare-webapp
           await-before-start]}]

  (system/await-before-start await-before-start)

  (let [prepare-webapp (or prepare-webapp identity)]
    (reduce (fn [server, {:keys [webapp-is-enabled] :or {webapp-is-enabled true} :as webapp}]
              (if webapp-is-enabled
                (start-webapp server (prepare-webapp webapp) options)
                (skip-webapp server webapp)))
            (-> (or options {})
                (with-meta {:running-webapps []}))
            webapps)))

(defn- stop-server
  [server]
  (web/stop server))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :app.system.service/immutant-web
  [_ options]
  (e/future (start-server options)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/halt-key! :app.system.service/immutant-web
  [_ server]
  ;; Stop service synchronously to continue shutdown of other systems when server is fully stopped.
  (stop-server (e/unwrap-future server)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
