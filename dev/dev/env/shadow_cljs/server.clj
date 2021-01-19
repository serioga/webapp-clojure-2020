(ns dev.env.shadow-cljs.server
  (:require [clojure.tools.logging :as log]
            [shadow.cljs.devtools.api :as api]
            [shadow.cljs.devtools.server :as server]
            [shadow.cljs.devtools.server.runtime :as server-runtime]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn start!
  "Starts Shadow CLJS server."
  [{:keys [builds-to-start] :as options}]
  (log/info "Start Shadow CLJS" options)
  (server/start!)
  (doseq [build builds-to-start]
    (api/watch build))
  (server-runtime/get-instance))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn stop!
  "Stops Shadow CLJS server."
  [instance]
  (log/info "Stop Shadow CLJS" instance)
  (server/stop!))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
