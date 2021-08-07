(ns dev.env.system.integrant.watcher
  (:require [dev.env.reload.watcher :as watcher]
            [integrant.core :as ig]
            [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :dev.env.system.integrant/watcher
  [_ {:keys [handler, options, handler-run-on-init]}]
  (let [watcher (watcher/start-watcher handler options)]
    (when handler-run-on-init
      (e/try-log-error ["Run handler on init" handler options]
        (handler :init-watcher)))
    watcher))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/halt-key! :dev.env.system.integrant/watcher
  [_ watcher]
  (watcher/stop-watcher watcher))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
