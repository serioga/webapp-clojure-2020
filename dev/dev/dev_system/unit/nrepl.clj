(ns dev.dev-system.unit.nrepl
  (:require
    [app.lib.util.exec :as exec]
    [clojure.tools.logging :as log]
    [integrant.core :as ig]
    [nrepl.server :as nrepl]))

(set! *warn-on-reflection* true)


(defn start-server
  [{:keys [port]}]
  (let [server (nrepl/start-server :port port)]
    (log/info "[DONE] Start nREPL server" server)
    server))


(defn stop-server
  [server]
  (log/info "Stop nREPL server" server)
  (nrepl/stop-server server))


#_(comment
    (time (let [server (time (start-server {}))]
            (time (stop-server server)))))


(defmethod ig/init-key :dev-system/*nrepl
  [_ options]
  (exec/future
    (start-server options)))


(defmethod ig/halt-key! :dev-system/*nrepl
  [_ *server]
  (stop-server @*server))


