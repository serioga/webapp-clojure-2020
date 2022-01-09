(ns dev.env.system.integrant.app-reload
  (:require [dev.env.reload.app-reload :as app-reload]
            [dev.env.system.app :as app.system]
            [integrant.core :as ig]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- build-watch-handler
  [config]
  (app-reload/watch-handler (assoc config :app-stop #'app.system/suspend
                                          :app-start #'app.system/resume
                                          :on-success #'app-reload/log-reload-success
                                          :on-failure #'app-reload/log-reload-failure)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :dev.env.system.integrant/app-reload
  [_ {:keys [watcher]}]
  (ig/init-key :dev.env.system.integrant/watcher
               (-> watcher (update :handler build-watch-handler))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/halt-key! :dev.env.system.integrant/app-reload
  [_ watcher]
  (ig/halt-key! :dev.env.system.integrant/watcher watcher))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
