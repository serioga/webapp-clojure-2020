(ns app.app-system.service.immutant-web
  (:require
    [app.app-system.impl :as impl]
    [app.lib.util.exec :as exec]
    [clojure.tools.logging :as log]
    [immutant.web :as web]
    [integrant.core :as ig]))

(set! *warn-on-reflection* true)


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

  (impl/await-before-start await-before-start)

  (let [prepare-webapp (or prepare-webapp identity)]
    (reduce (fn [server, {:keys [enabled?] :or {enabled? true} :as webapp}]
              (if enabled?
                (start-webapp server (prepare-webapp webapp) options)
                (skip-webapp server webapp)))
            (-> (or options {})
                (with-meta {:running-webapps []}))
            webapps)))


(defn- stop-server
  [server]
  (web/stop server))


(defmethod ig/init-key :app-system.service/ref'immutant-web
  [_ options]
  (exec/future (start-server options)))


(defmethod ig/halt-key! :app-system.service/ref'immutant-web
  [_ ref'server]
  (exec/future (stop-server @ref'server)))
