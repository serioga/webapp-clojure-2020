(ns dev.env.nrepl.server
  (:require [clojure.java.io :as io]
            [nrepl.server :as nrepl]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn start-server
  "Starts nREPL server."
  [{:keys [port, write-port-file]}]
  (let [server (nrepl/start-server :port port)]
    (when (some? write-port-file)
      (let [nrepl-port-file (io/file write-port-file)]
        (spit nrepl-port-file (str (:port server)))
        (.deleteOnExit nrepl-port-file)))
    server))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn stop-server
  "Stops nREPL server."
  [server]
  (nrepl/stop-server server))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(comment
  (time (let [server (time (start-server {}))]
          (time (stop-server server)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
