(ns dev.env.shadow-cljs.server
  (:require [shadow.cljs.devtools.api :as api]
            [shadow.cljs.devtools.server :as server]
            [shadow.cljs.devtools.server.runtime :as runtime]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn start
  "Starts Shadow CLJS server."
  [{:keys [builds-to-start]}]
  (server/start!)
  (doseq [build builds-to-start]
    (api/watch build))
  (runtime/get-instance))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn stop!
  "Stops Shadow CLJS server."
  [_]
  (server/stop!))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
