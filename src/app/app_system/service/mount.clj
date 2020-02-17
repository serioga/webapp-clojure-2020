(ns app.app-system.service.mount
  (:require
    [integrant.core :as ig]
    [mount.core :as mount]
    [mount-up.core :as mu]))

(set! *warn-on-reflection* true)


(mu/on-up :info mu/log :before)


(defmethod ig/init-key :app-system.service/mount
  [_ args]
  (try
    (mount/start-with-args args)
    (catch Throwable ex
      (mount/stop)
      (throw ex))))


(defmethod ig/halt-key! :app-system.service/mount
  [_ _]
  (mount/stop))

