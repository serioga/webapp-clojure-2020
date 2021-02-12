(ns dev.env.system.integrant.shadow-cljs
  (:require [dev.env.shadow-cljs.server :as server]
            [integrant.core :as ig]
            [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(derive :dev.env.system.integrant/shadow-cljs
        :lib.integrant.system/keep-running-on-suspend)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :dev.env.system.integrant/shadow-cljs
  [_ options]
  (e/future (server/start! options)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/halt-key! :dev.env.system.integrant/shadow-cljs
  [_ server]
  (e/future (server/stop! (e/unwrap-future server))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
