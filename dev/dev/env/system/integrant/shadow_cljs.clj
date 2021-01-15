(ns dev.env.system.integrant.shadow-cljs
  (:require [dev.env.shadow-cljs.server :as server]
            [integrant.core :as ig]
            [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :dev.env.system/ref'shadow-cljs
  [_ options]
  (e/future (server/start! options)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/halt-key! :dev.env.system/ref'shadow-cljs
  [_ ref'server]
  (e/future (server/stop! @ref'server)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
