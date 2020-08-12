(ns dev.app.system.unit.nrepl
  (:require
    [app.lib.util.exec :as e]
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [integrant.core :as ig]
    [nrepl.server :as nrepl]))

(set! *warn-on-reflection* true)


(defn- start-server
  [{:keys [port, write-port-file]}]
  (let [server (nrepl/start-server :port port)]
    (log/info "[DONE] Start nREPL server" server)

    (when (some? write-port-file)
      (let [nrepl-port-file (io/file write-port-file)]
        (spit nrepl-port-file (str (:port server)))
        (.deleteOnExit nrepl-port-file)))

    server))


(defn- stop-server
  [server]
  (log/info "Stop nREPL server" server)
  (nrepl/stop-server server))


(comment
  (time (let [server (time (start-server {}))]
          (time (stop-server server)))))


(defmethod ig/init-key :dev-system/ref'nrepl
  [_ options]
  (e/future (start-server options)))


(defmethod ig/halt-key! :dev-system/ref'nrepl
  [_ ref'server]
  (stop-server @ref'server))


