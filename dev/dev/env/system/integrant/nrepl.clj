(ns dev.env.system.integrant.nrepl
  (:require [dev.env.nrepl.server :as nrepl]
            [integrant.core :as ig]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(derive :dev.env.system.integrant/nrepl
        :lib.integrant.system/keep-running-on-suspend)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :dev.env.system.integrant/nrepl
  [_ options]
  (nrepl/start-server options))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/halt-key! :dev.env.system.integrant/nrepl
  [_ server]
  (nrepl/stop-server server))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
