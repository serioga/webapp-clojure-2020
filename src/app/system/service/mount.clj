(ns app.system.service.mount
  (:require [integrant.core :as ig]
            [lib.clojure.core :as e]
            [mount-up.core :as mu]
            [mount.core :as mount]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(mu/on-up :info mu/log :before)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :app.system.service/mount
  [_ args]
  (e/future
    (try
      (mount/start-with-args (->> args (e/map-vals e/unwrap-future)))
      (catch Throwable e
        (mount/stop)
        (throw e)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/halt-key! :app.system.service/mount
  [_ _]
  (e/future
    (mount/stop)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
