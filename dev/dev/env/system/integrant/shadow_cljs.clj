(ns dev.env.system.integrant.shadow-cljs
  (:require [dev.env.shadow-cljs.server :as server]
            [integrant.core :as ig]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(derive :dev.env.system.integrant/shadow-cljs
        :lib.integrant.system/keep-running-on-suspend)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :dev.env.system.integrant/shadow-cljs
  [_ options]
  (server/start options))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/halt-key! :dev.env.system.integrant/shadow-cljs
  [_ server]
  (server/stop! server))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
