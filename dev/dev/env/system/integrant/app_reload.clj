(ns dev.env.system.integrant.app-reload
  (:require [dev.env.reload.app :as app-reload]
            [integrant.core :as ig]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :dev.env.system/app-reload
  [_ options]
  (app-reload/watcher-handler options))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
