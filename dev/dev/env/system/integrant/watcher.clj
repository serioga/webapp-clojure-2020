(ns dev.env.system.integrant.watcher
  (:require [dev.env.reload.watcher :as watcher]
            [integrant.core :as ig]
            [lib.clojure-tools-logging.logger :as logger]
            [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private logger (logger/get-logger *ns*))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :dev.env.system.integrant/watcher
  [_ {:keys [handler, options, handler-run-on-init]}]
  (let [watcher (watcher/start-watcher handler options)]
    (when handler-run-on-init
      (try
        (handler :init-watcher)
        (catch Throwable e
          (logger/log-throwable logger e (e/pr-str* "Run handler on init" handler options)))))
    watcher))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/halt-key! :dev.env.system.integrant/watcher
  [_ watcher]
  (watcher/stop-watcher watcher))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
